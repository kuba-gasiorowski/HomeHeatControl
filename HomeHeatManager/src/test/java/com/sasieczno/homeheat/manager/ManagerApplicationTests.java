package com.sasieczno.homeheat.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sasieczno.homeheat.manager.model.CircuitMode;
import com.sasieczno.homeheat.manager.model.ControllerProcessData;
import com.sasieczno.homeheat.manager.model.HeatStatus;
import com.sasieczno.homeheat.manager.model.HeatingPeriod;
import com.sasieczno.homeheat.manager.repository.ControllerRepository;
import com.sasieczno.homeheat.manager.security.AuthData;
import com.sasieczno.homeheat.manager.service.ControllerStatusService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		classes = {ManagerApplication.class})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("IntegrationTests")
class ManagerApplicationTests {

	@MockBean
	ControllerRepository controllerRepository;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ControllerStatusService controllerStatusService;

	@Value("${manager.udp.port}")
	int managerPort;

	static String accessToken = "";
	static String refreshToken = "";

	static String oldRefreshToken = "";

	@BeforeEach
	public void copyProperties() throws Exception {
		Files.copy(Path.of("src/test/resources/HomeHeat.yml"), Path.of("build/tmp/test/HomeHeat.yml"), StandardCopyOption.REPLACE_EXISTING);
	}

	@Test
	@Order(1)
	public void getStatusUnauthorized() throws Exception {
		this.mockMvc.perform(get("/api/status")).andDo(print())
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Order(1)
	public void getWhateverApiUnauthorized() throws Exception {
		this.mockMvc.perform(get("/api/nonexistent")).andDo(print())
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Order(2)
	public void loginAdminOk() throws Exception {
		mockMvc.perform(post("/api/auth/login").contentType("application/json; charset=utf-8").content("{\"username\":\"admin\",\"password\":\"password1\"}"))
				.andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					AuthData authData = convertJsonToObject(json, AuthData.class);
					accessToken = authData.getToken();
					refreshToken = authData.getRefreshToken();
				});
	}

	@Test
	@Order(3)
	public void getStatusOkInactive() throws Exception {
		final AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};

		long hourAgo = System.currentTimeMillis() - 3600000L;
		long now = System.currentTimeMillis();
		ControllerProcessData cpd = new ControllerProcessData();
		cpd.setStatus("inactive");
		cpd.setNRestarts(1);
		cpd.setPid(121);
		cpd.setActiveStateTimestamp(hourAgo);
		cpd.setInactiveStateTimestamp(now);
		Mockito.when(controllerRepository.getControllerProcessData())
						.thenReturn(cpd);
		mockMvc.perform(get("/api/status").header("Authorization", "Bearer " + accessToken)).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(now, result.get().get("lastStatusChangeTime"));
		Assertions.assertEquals(false, result.get().get("controllerStatus"));
	}

	@Test
	@Order(3)
	public void getStatusOkActive() throws Exception {
		final AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};

		long hourAgo = System.currentTimeMillis() - 3600000L;
		long now = System.currentTimeMillis();
		ControllerProcessData cpd = new ControllerProcessData();
		cpd.setStatus("active");
		cpd.setNRestarts(1);
		cpd.setPid(121);
		cpd.setActiveStateTimestamp(hourAgo);
		cpd.setInactiveStateTimestamp(now);
		Mockito.when(controllerRepository.getControllerProcessData())
				.thenReturn(cpd);
		mockMvc.perform(get("/api/status").header("Authorization", "Bearer " + accessToken)).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(hourAgo, result.get().get("lastStatusChangeTime"));
		Assertions.assertTrue((Boolean) result.get().get("controllerStatus"));
	}

	@Test
	@Order(4)
	public void getStatusOkActiveData() throws Exception {
		final AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {};

		long hourAgo = System.currentTimeMillis() - 3600000L;
		double now = ((double) System.currentTimeMillis()) / 1000.0;
		ControllerProcessData cpd = new ControllerProcessData();
		cpd.setStatus("active");
		cpd.setNRestarts(1);
		cpd.setPid(121);
		cpd.setActiveStateTimestamp(hourAgo);
		Mockito.when(controllerRepository.getControllerProcessData())
				.thenReturn(cpd);

		prepareAndSendCtlDatagram(now, (byte)0, -1.1, 2.5);
		// make sure the server processes the msg
		Thread.sleep(200);

		mockMvc.perform(get("/api/status").header("Authorization", "Bearer " + accessToken)).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(hourAgo, result.get().get("lastStatusChangeTime"));
		Assertions.assertEquals((long)(now*1000.0), result.get().get("lastMessageTime"));
		Assertions.assertEquals(HeatingPeriod.NO_HEATING.name(), result.get().get("heatingPeriod"));
		Assertions.assertEquals(-1.1, result.get().get("externalTemperature"));
		Assertions.assertEquals(2.5, result.get().get("avgExternalTemperature"));
	}

	@Test
	@Order(5)
	public void getConfigOk() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

		mockMvc.perform(get("/api/config").header("Authorization", "Bearer " + accessToken)).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(-18.0, result.get().get("extMinTemp"));
		Assertions.assertEquals(12.0, result.get().get("extMaxTemp"));
		Assertions.assertEquals(0.15, result.get().get("extStartThreshold"));
		Assertions.assertEquals("config/HomeHeat_logging.yml", result.get().get("logConfig"));
		convertAndCheckLocaltime(LocalTime.of(22, 0, 0), result.get().get("nightStartTime"));
		convertAndCheckLocaltime(LocalTime.of(6, 58, 0), result.get().get("nightEndTime"));
		convertAndCheckLocaltime(LocalTime.of(13, 0, 0), result.get().get("dayStartTime"));
		convertAndCheckLocaltime(LocalTime.of(15, 58, 0), result.get().get("dayEndTime"));
		Assertions.assertTrue(result.get().get("circuits") instanceof List);
		List<Object> circuits = (List<Object>) result.get().get("circuits");
		Assertions.assertEquals(10, circuits.size());
		checkCircuit(0, "kitchen", CircuitMode.DAY, 35.0, 20.0, null, null, circuits.get(0));
		checkCircuit(1, "living room", CircuitMode.NIGHT, 30.0, 20.0, null, null, circuits.get(1));
		checkCircuit(8, "bathroom", CircuitMode.OFF, 29.0, 11.0, -0.1, 0.15, circuits.get(8));
		checkCircuit(9, "technical room", CircuitMode.ALL, 35.0, 20.0, null, null, circuits.get(9));
	}

	@Test
	@Order(6)
	public void updateConfigOkOneProperty1() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

		mockMvc.perform(post("/api/config").header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"extMaxTemp\":15.0}")).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(-18.0, result.get().get("extMinTemp"));
		Assertions.assertEquals(15.0, result.get().get("extMaxTemp"));
		Assertions.assertEquals(0.15, result.get().get("extStartThreshold"));
		Assertions.assertEquals("config/HomeHeat_logging.yml", result.get().get("logConfig"));
		convertAndCheckLocaltime(LocalTime.of(22, 0, 0), result.get().get("nightStartTime"));
		convertAndCheckLocaltime(LocalTime.of(6, 58, 0), result.get().get("nightEndTime"));
		convertAndCheckLocaltime(LocalTime.of(13, 0, 0), result.get().get("dayStartTime"));
		convertAndCheckLocaltime(LocalTime.of(15, 58, 0), result.get().get("dayEndTime"));
		Assertions.assertTrue(result.get().get("circuits") instanceof List);
		List<Object> circuits = (List<Object>) result.get().get("circuits");
		Assertions.assertEquals(10, circuits.size());
		checkCircuit(0, "kitchen", CircuitMode.DAY, 35.0, 20.0, null, null, circuits.get(0));
		checkCircuit(1, "living room", CircuitMode.NIGHT, 30.0, 20.0, null, null, circuits.get(1));
		checkCircuit(8, "bathroom", CircuitMode.OFF, 29.0, 11.0, -0.1, 0.15, circuits.get(8));
		checkCircuit(9, "technical room", CircuitMode.ALL, 35.0, 20.0, null, null, circuits.get(9));
	}

	@Test
	@Order(7)
	public void updateConfigOkPropertyCircuit() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

		mockMvc.perform(post("/api/config").header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dayStartTime\":[14,30],\"circuits\":[{\"index\":1,\"description\":\"newroom\",\"maxTemp\":29}]}")).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(-18.0, result.get().get("extMinTemp"));
		Assertions.assertEquals(12.0, result.get().get("extMaxTemp"));
		Assertions.assertEquals(0.15, result.get().get("extStartThreshold"));
		Assertions.assertEquals("config/HomeHeat_logging.yml", result.get().get("logConfig"));
		convertAndCheckLocaltime(LocalTime.of(22, 0, 0), result.get().get("nightStartTime"));
		convertAndCheckLocaltime(LocalTime.of(6, 58, 0), result.get().get("nightEndTime"));
		convertAndCheckLocaltime(LocalTime.of(14, 30, 0), result.get().get("dayStartTime"));
		convertAndCheckLocaltime(LocalTime.of(15, 58, 0), result.get().get("dayEndTime"));
		Assertions.assertTrue(result.get().get("circuits") instanceof List);
		List<Object> circuits = (List<Object>) result.get().get("circuits");
		Assertions.assertEquals(10, circuits.size());
		checkCircuit(0, "kitchen", CircuitMode.DAY, 35.0, 20.0, null, null, circuits.get(0));
		checkCircuit(1, "newroom", CircuitMode.NIGHT, 29.0, 20.0, null, null, circuits.get(1));
		checkCircuit(8, "bathroom", CircuitMode.OFF, 29.0, 11.0, -0.1, 0.15, circuits.get(8));
		checkCircuit(9, "technical room", CircuitMode.ALL, 35.0, 20.0, null, null, circuits.get(9));
	}

	@Test
	@Order(20)
	public void getCircuit2() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

		mockMvc.perform(get("/api/config/circuit/2").header("Authorization", "Bearer " + accessToken)).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(2, result.get().get("index"));
		Assertions.assertEquals("boys' room 1", result.get().get("description"));
		Assertions.assertEquals(CircuitMode.ALL, CircuitMode.valueOf((String)result.get().get("active")));
		Assertions.assertEquals(30.0, result.get().get("maxTemp"));
		Assertions.assertEquals(20.0, result.get().get("tempBaseLevel"));
		Assertions.assertTrue(result.get().containsKey("nightAdjust"));
		Assertions.assertNull(result.get().get("nightAdjust"));
		Assertions.assertTrue(result.get().containsKey("dayAdjust"));
		Assertions.assertNull(result.get().get("dayAdjust"));
	}

	@Test
	@Order(20)
	public void getCircuit8() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

		mockMvc.perform(get("/api/config/circuit/8").header("Authorization", "Bearer " + accessToken)).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(8, result.get().get("index"));
		Assertions.assertEquals("bathroom", result.get().get("description"));
		Assertions.assertEquals(CircuitMode.OFF, CircuitMode.valueOf((String)result.get().get("active")));
		Assertions.assertEquals(29.0, result.get().get("maxTemp"));
		Assertions.assertEquals(11.0, result.get().get("tempBaseLevel"));
		Assertions.assertEquals(-0.1, result.get().get("nightAdjust"));
		Assertions.assertEquals(0.15, result.get().get("dayAdjust"));
		Assertions.assertTrue(((List) result.get().get("heatCharacteristics")).size() > 0);
	}

	@Test
	@Order(21)
	public void setCircuit8ActiveOnly() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

		mockMvc.perform(post("/api/config/circuit/8").header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content("{\"index\":8,\"active\": \"DAY\"}")).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(8, result.get().get("index"));
		Assertions.assertEquals("bathroom", result.get().get("description"));
		Assertions.assertEquals(CircuitMode.DAY, CircuitMode.valueOf((String)result.get().get("active")));
		Assertions.assertEquals(29.0, result.get().get("maxTemp"));
		Assertions.assertEquals(11.0, result.get().get("tempBaseLevel"));
		Assertions.assertEquals(-0.1, result.get().get("nightAdjust"));
		Assertions.assertEquals(0.15, result.get().get("dayAdjust"));
		Assertions.assertTrue(((List) result.get().get("heatCharacteristics")).size() > 0);
	}

	@Test
	@Order(22)
	public void setCircuit8MoreProps() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

		mockMvc.perform(post("/api/config/circuit/8").header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content("{\"index\":8,\"active\":\"NIGHT\",\"maxTemp\":28.5,\"nightAdjust\":0.3,\"dayAdjust\":0}")).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(objectMapper.readValue(json, typeRef));
				});
		Assertions.assertEquals(8, result.get().get("index"));
		Assertions.assertEquals("bathroom", result.get().get("description"));
		Assertions.assertEquals(CircuitMode.NIGHT, CircuitMode.valueOf((String)result.get().get("active")));
		Assertions.assertEquals(28.5, result.get().get("maxTemp"));
		Assertions.assertEquals(11.0, result.get().get("tempBaseLevel"));
		Assertions.assertEquals(0.3, result.get().get("nightAdjust"));
		Assertions.assertEquals(0.0, result.get().get("dayAdjust"));
		Assertions.assertTrue(((List) result.get().get("heatCharacteristics")).size() > 0);
	}

	@Test
	@Order(23)
	public void setCircuit8WrongIndex() throws Exception {
		AtomicReference<HashMap<String, Object>> result = new AtomicReference<>();
		final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};

		mockMvc.perform(post("/api/config/circuit/8").header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content("{\"index\":1,\"active\":\"ALL\"}")).andDo(print())
				.andExpect(status().is4xxClientError());
	}


	@Test
	@Order(80)
	public void refreshToken() throws Exception {
		oldRefreshToken = refreshToken;
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.TEXT_PLAIN_VALUE)
				.content(refreshToken))
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					AuthData authData = convertJsonToObject(mvcResult.getResponse().getContentAsString(), AuthData.class);
					accessToken = authData.getToken();
					refreshToken = authData.getRefreshToken();
				});
	}

	@Test
	@Order(81)
	public void refreshTokenOldFails() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.TEXT_PLAIN_VALUE)
						.content(oldRefreshToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@Order(82)
	public void refreshTokenAgain() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.TEXT_PLAIN_VALUE)
						.content(refreshToken))
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					AuthData authData = convertJsonToObject(mvcResult.getResponse().getContentAsString(), AuthData.class);
					accessToken = authData.getToken();
					refreshToken = authData.getRefreshToken();
				});
	}

	@Test
	@Order(83)
	public void getStatusWithNewToken() throws Exception {
		AtomicReference<HeatStatus> result = new AtomicReference<>();
		ControllerProcessData cpd = new ControllerProcessData();
		cpd.setStatus("active");
		cpd.setNRestarts(1);
		cpd.setPid(121);
		cpd.setActiveStateTimestamp(System.currentTimeMillis());
		cpd.setInactiveStateTimestamp(System.currentTimeMillis()-600*1000);
		Mockito.when(controllerRepository.getControllerProcessData())
				.thenReturn(cpd);
		mockMvc.perform(get("/api/status").header("Authorization", "Bearer " + accessToken)).andDo(print())
				.andExpect(status().is2xxSuccessful())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andDo(mvcResult -> {
					String json = mvcResult.getResponse().getContentAsString();
					result.set(convertJsonToObject(json, HeatStatus.class));
				});
		Assertions.assertEquals(-1.1, result.get().getExternalTemperature());
	}

	@Test
	@Order(90)
	public void logout() throws Exception {
		mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessToken)
						.contentType(MediaType.TEXT_PLAIN_VALUE).content(refreshToken))
				.andExpect(status().is2xxSuccessful());
	}

	@Test
	@Order(91)
	public void refreshTokenAfterLogout() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.TEXT_PLAIN_VALUE)
				.content(refreshToken))
				.andExpect(status().isUnauthorized());
	}

	public <T> T convertJsonToObject(String json, Class<T> object) throws IOException {
		return objectMapper.readValue(json, object);
	}

	void convertAndCheckLocaltime(LocalTime expected, Object src) throws IllegalArgumentException {
		String data;
		Assertions.assertTrue(String.class.isAssignableFrom(src.getClass()));
		data = (String) src;
		List<String> parts = List.of(data.split(":"));
        Assertions.assertEquals(3, parts.size());
		LocalTime lt = LocalTime
				.of(Integer.parseInt(parts.get(0)),
						Integer.parseInt(parts.get(1)),
						Integer.parseInt(parts.get(2)));
		Assertions.assertEquals(expected, lt);
	}

	void checkCircuit(Integer index, String description, CircuitMode active, Double maxTemp, Double tempBaseLevel,
					  Double nightAdjust, Double dayAdjust, Object src) {
		HashMap<String, Object> circuit = new HashMap<>();
		Assertions.assertTrue(circuit.getClass().isAssignableFrom(src.getClass()));
		circuit = (HashMap<String, Object>) src;
		Assertions.assertEquals(index, circuit.get("index"));
		Assertions.assertEquals(description, circuit.get("description"));
		Assertions.assertEquals(active, CircuitMode.valueOf((String)circuit.get("active")));
		Assertions.assertEquals(maxTemp, circuit.get("maxTemp"));
		Assertions.assertEquals(tempBaseLevel, circuit.get("tempBaseLevel"));
		Assertions.assertEquals(nightAdjust, circuit.get("nightAdjust"));
		Assertions.assertEquals(dayAdjust, circuit.get("dayAdjust"));
	}

	void prepareAndSendCtlDatagram(double timestamp, byte heatingPeriod, double extTemp, double avgExtTemp) throws IOException {
		DatagramSocket client = new DatagramSocket();
		InetAddress address = InetAddress.getByName("127.0.0.1");
		ByteBuffer bb = ByteBuffer.allocate(4096);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.putDouble(timestamp);
		bb.put(heatingPeriod);
		bb.putDouble(extTemp);
		bb.putDouble(avgExtTemp);
		bb.flip();

		DatagramPacket packet = new DatagramPacket(bb.array(), bb.array().length, address, managerPort);
		client.send(packet);
		client.close();
	}

}

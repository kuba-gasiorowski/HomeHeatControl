#!/usr/bin/env bash

# Inspirations:
# https://gist.github.com/cha55son/6042560
# https://silvinux.wordpress.com/2015/01/04/dynamic-motd-centosredhat/
#

USER=`whoami`
HOSTNAME=`uname -n`
IP_outtest=`hostname --all-ip-addresses 2>/dev/null | wc -l`;
if [[ $IP_outtest == 0 ]]; then
  IP=`hostname --ip-address`;
else
  IP=`hostname --all-ip-addresses`;
fi

# disk usage on /
ROOT=`df -Ph / | awk '/\// {print "used:",$3,"/",$2," ( free:",$4,")"}'`
ROOT_USAGE=`df -h / | awk '/\// {print $5}'|grep -v "^$"`

# get memory details and percentage used
MEMORY_outtest=`free -mh 2>/dev/null | wc -l`;
if [[ $MEMORY_outtest == 0 ]]; then
  MEMORY_cmd="free -m";
else
  MEMORY_cmd="free -mh";
fi
MEMORY=`$MEMORY_cmd | grep "Mem" | awk '{print "used:",$3,"/",$2,"( free: ",$7,")"}'`
MEM_USAGE=`free -m | grep "Mem" | awk '{printf("%3.1f%%", (($3/$2)*100))}'`

# get swap file details
SWAP=`$MEMORY_cmd | grep "Swap" | awk '{print "used:",$3,"/",$2,"( free: ",$4,")"}'`

# get processes
PSA=`ps -Afl | wc -l`

# time of day
HOUR=$(date +"%H")
if [ $HOUR -lt 12 -a $HOUR -ge 0 ]
then TIME="morning"
elif [ $HOUR -lt 17 -a $HOUR -ge 12 ]
then TIME="afternoon"
else
TIME="evening"
fi

osver="";
if [ -f "/etc/redhat-release" ]; then
  osver=`cat /etc/redhat-release`;
elif [ -f "/etc/os-release" ]; then
  osname=`cat /etc/os-release | grep "^NAME=" | cut -d'"' -f2`
  osversion=`cat /etc/os-release | grep "^VERSION=" | cut -d'"' -f2`
  osver="$osname $osversion";
fi

# system uptime
uptime=`cat /proc/uptime | cut -f1 -d.`
upDays=$((uptime/60/60/24))
upHours=$((uptime/60/60%24))
upMins=$((uptime/60%60))
upSecs=$((uptime%60))

# system load (1/5/15 minutes)
LOAD1=`cat /proc/loadavg | awk {'print $1'}`
LOAD5=`cat /proc/loadavg | awk {'print $2'}`
LOAD15=`cat /proc/loadavg | awk {'print $3'}`

COLOR_COLUMN="\e[1m-"
COL_WHITE="\e[39m"
COL_RED="\e[31m"
COL_GREEN="\e[32m"
COL_YELLOW="\e[33m"
RESET_COLORS="\e[0m"

RED='\033[01;31m'
GREEN='\033[01;32m'
YELLOW='\033[01;33m'
BLUE='\033[01;34m'
NONE='\033[0m'

kernelRelease=`uname -r`
kernelProcessor=`uname -p`
kernelVersion=`uname -v`

#userList=`who | cut -d' ' -f1,12 | sort -u`
#userListSize=`cat $userList | wc -l`
#uList="";i=1;
#for u in $userList; do
#  uList="$uList$u";
#  if [ $i < "$userListSize"]; then
#    i=$i+1;
#    uList="$uList,";
#  fi
#done

#d=`df -Ph | grep -v tmpfs | grep -v Filesystem| awk '{print "   used:",$3,"/",$2,"(free:",$4,")","\t: ",$6,"   \tsource:",$1}'`;
#d=`df -Ph | grep -v tmpfs | grep -v Filesystem| awk '{print "   used:",$3,"/",$2,"(free:",$4,")","\t: ",$6}'`;
d=`df -Ph | grep -v tmpfs | grep -v Filesystem| awk -v r=$RED -v y=$YELLOW -v n=$NONE '{OFS = ""; ORS = "";print "   ","used: ";
if (substr($5,0,2)<90) print $5," ",$3," / ",$2,"(free: ",$4,")","\t: ",$6,"\n";
else if (substr($5,0,2)>=95) print r,$5," ",$3,n," / ",$2,"(free: ",r,$4,n,")","\t: ",r,$6,"\tCRITICAL",n,"\n";
else print y,$5," ",$3,n," / ",$2,"(free: ",y,$4,n,")","\t: ",y,$6,"\tWARNING",n,"\n";
}'`;

# see: https://patorjk.com/software/taag/#p=display&c=echo&f=Big&t=HEATCONTROL
echo "  _    _ ______       _______ _____ ____  _   _ _______ _____   ____  _      ";
echo " | |  | |  ____|   /\|__   __/ ____/ __ \| \ | |__   __|  __ \ / __ \| |     ";
echo " | |__| | |__     /  \  | | | |   | |  | |  \| |  | |  | |__) | |  | | |     ";
echo " |  __  |  __|   / /\ \ | | | |   | |  | | . \` |  | |  |  _  /| |  | | |     ";
echo " | |  | | |____ / ____ \| | | |___| |__| | |\  |  | |  | | \ \| |__| | |____ ";
echo " |_|  |_|______/_/    \_\_|  \_____\____/|_| \_|  |_|  |_|  \_\\____/|______|";
echo "                                                                             ";
echo "                                                                             ";
echo -e "=============================== SYSTEM ====================================
$COLOR_COLUMN- Current user$RESET_COLORS \t:$COL_WHITE $USER $RESET_COLORS
$COLOR_COLUMN- Hostname$RESET_COLORS \t\t:$COL_YELLOW $HOSTNAME $RESET_COLORS
$COLOR_COLUMN- IP Address$RESET_COLORS \t\t:$COL_YELLOW $IP $RESET_COLORS
$COLOR_COLUMN- Operating System$RESET_COLORS \t:$COL_GREEN $osver $RESET_COLORS
$COLOR_COLUMN- Kernel$RESET_COLORS \t\t:$COL_WHITE $kernelRelease $kernelProcessor
\t\t\t  $kernelVersion $RESET_COLORS
$COLOR_COLUMN- System uptime$RESET_COLORS \t:$COL_WHITE $upDays days, $upHours hours, $upMins minutes, $upSecs seconds $RESET_COLORS
$COLOR_COLUMN- Users$RESET_COLORS \t\t:$COL_WHITE Currently `users | wc -w` user(s) logged on $RESET_COLORS
============================== SYSINFO ==================================$RESET_COLORS
$COLOR_COLUMN- CPU usage$RESET_COLORS \t\t:$COL_WHITE $LOAD1 - $LOAD5 - $LOAD15 (1-5-15 min) $RESET_COLORS
$COLOR_COLUMN- Memory usage$RESET_COLORS \t:$COL_WHITE $MEM_USAGE $RESET_COLORS
$COLOR_COLUMN- Memory$RESET_COLORS \t\t:$COL_WHITE $MEMORY $RESET_COLORS
$COLOR_COLUMN- Swap in use$RESET_COLORS \t\t:$COL_WHITE $SWAP $RESET_COLORS
$COLOR_COLUMN- Processes$RESET_COLORS \t\t:$COL_WHITE $PSA running $RESET_COLORS
$COLOR_COLUMN- Disk space $RESET_COLORS "
echo -e "$d $RESET_COLORS";
echo -e "=============================== SERVICES ==================================="
systemctl --no-pager status HomeHeat.service
echo -e "$RESET_COLORS"
systemctl --no-pager status HomeHeatManager.service
echo -e "$RESET_COLORS"
echo -e "==========================================================================="
package com.sasieczno.homeheat.manager.repository.impl;

/**
 * The class is necessary to avoid quoting 'OFF' value in YAML files.
 */
public class StringQuotingChecker extends com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker.Default {
    @Override
    public boolean needToQuoteValue(String value) {
        if ("OFF".equals(value))
            return false;
        else
            return super.needToQuoteValue(value);
    }
}

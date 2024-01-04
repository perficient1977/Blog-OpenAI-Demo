package com.perficient.aem.sample.openai.core.services;

import com.fasterxml.jackson.databind.util.JSONPObject;

/**
 * Interface to deal with Json.
 */
public interface JSONConverter {

    Object convertToObject(JSONPObject jsonpObject, Class clazz);

    Object convertToObject(String jsonString, Class clazz);
    String convertToJsonString(Object object);
    JSONPObject convertToJSONPObject(Object object);
}

package com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.model.DynatraceAction;
import com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.model.DynatraceVisit;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasrotte on 20/11/2018.
 */
public class Parser {

    public static DynatraceVisit parseVisit(String entry) {
        DynatraceVisit visit = new DynatraceVisit();
        visit.setOriginalString(entry);
        JsonElement element = new JsonParser().parse(entry);
        if (element.getAsJsonObject().get("userSessionId") ==null){return null;}

        try {
            visit.setId(element.getAsJsonObject().get("userSessionId").getAsString());
        } catch (Exception e) {
        }

        List<DynatraceAction> actions = new ArrayList<>();

        for (JsonElement actionElement :
            element.getAsJsonObject().get("userActions").getAsJsonArray()) {
            DynatraceAction action =  new DynatraceAction();
            action.setOriginalString(actionElement.toString());
            try {
                action.setApplication(actionElement.getAsJsonObject().get("application").getAsString());
                visit.setApplication(actionElement.getAsJsonObject().get("application").getAsString());
            } catch (Exception e) {
            }
            try {
                action.setActionName(actionElement.getAsJsonObject().get("name").getAsString());
            } catch (Exception e) {
            }
            try {
                action.setActionPrettyName(actionElement.getAsJsonObject().get("name").getAsString());
            } catch (Exception e) {
            }
            try {
                action.setType(actionElement.getAsJsonObject().get("type").getAsString());
            } catch (Exception e) {
            }
            try {
                action.setTargetUrl(actionElement.getAsJsonObject().get("targetUrl").getAsString());
            } catch (Exception e) {
            }
            try {
                action.setVisitId(visit.getId());
            } catch (Exception e) {
            }
            try {
                action.setStartTime(actionElement.getAsJsonObject().get("startTime").getAsLong());
            } catch (Exception e) {
            }
            try {
                action.setEndTime(actionElement.getAsJsonObject().get("endTime").getAsLong());
            } catch (Exception e) {
            }

            actions.add(action);
        }

        visit.setActions(actions);

        return visit;
    }



}

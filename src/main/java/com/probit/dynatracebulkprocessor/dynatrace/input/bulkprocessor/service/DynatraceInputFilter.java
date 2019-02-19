package com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.service;


import com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.model.DynatraceAction;
import com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.model.DynatraceVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DynatraceInputFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynatraceInputFilter.class);

    private List<String> APPLICATION_LIST = new ArrayList<>();
    private List<String> ACTIONS_TO_FILTER = new ArrayList<>();

    public DynatraceInputFilter(){
        if (System.getProperty("Applications")!=null){
            APPLICATION_LIST.addAll(Arrays.asList(System.getProperty("Applications").split(",")));
        }

        if (System.getProperty("Actions_to_filter")!=null){
            ACTIONS_TO_FILTER.addAll(Arrays.asList(System.getProperty("Actions_to_filter").split(",")));
        }

        LOGGER.info("Starting up with applicationList: " + concatenateList(APPLICATION_LIST, ","));
        LOGGER.info("Starting up with actionsToFilter: " + concatenateList(ACTIONS_TO_FILTER, ","));

    }

    public boolean isCorrectApplication(DynatraceVisit visit) {
        boolean isCorrectApplication = visit != null && visit.getApplication() != null && APPLICATION_LIST
            .contains(visit.getApplication());
        if (! isCorrectApplication){
            LOGGER.warn("incorrect application: found " + visit.getApplication() + " while allowed applications are limited to " + concatenateList(APPLICATION_LIST, ","));
        }
        return isCorrectApplication;
    }

    public boolean isActionToFilter(DynatraceAction action) {
        return (action != null && action.getActionName() != null && listContainsStringContains(
            action.getActionName()));
    }

    private boolean listContainsStringContains(String text) {
        for (String ActionToFilter : ACTIONS_TO_FILTER) {
            if(text.contains(ActionToFilter)) {
                return true;
            }
        }
        return false;
    }

    private String concatenateList(List<String> list, String separator){
        StringBuffer result = new StringBuffer();
        for (String s :
            list) {
            result.append(s);
            result.append(separator);
        }
        // todo: remove last separator (result.substring...)
        return result.toString();
    }

}

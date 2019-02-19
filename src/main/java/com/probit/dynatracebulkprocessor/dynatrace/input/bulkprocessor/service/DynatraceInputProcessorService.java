package com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.service;

import com.google.gson.Gson;
import com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.cache.DynatraceSessionRepository;
import com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.model.*;
import com.probit.dynatracebulkprocessor.dynatrace.input.bulkprocessor.util.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class DynatraceInputProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynatraceInputProcessorService.class);

    @Autowired
    private DynatraceInputFilter inputFilter;

    @Autowired
    private DynatraceSessionRepository sessionRepository;

    private Long startTime = new Date().getTime();
    private long delayInSeconds = 0;
    private static final Gson GSON = new Gson();

    private long minimumActionsForSession = 5;

    private String user = "probit";
    private String password = "probit";

    private String endPoint = "http://localhost:9094/session/enqueue";

    public DynatraceInputProcessorService(){
        if (System.getProperty("Delay")!=null){
            try {
                delayInSeconds = Long.parseLong(System.getProperty("Delay"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        if (System.getProperty("Endpoint")!=null){
            endPoint = System.getProperty("Endpoint");
        }
        if (System.getProperty("User")!=null){
            user = System.getProperty("User");
        }
        if (System.getProperty("Password")!=null){
            password = System.getProperty("Password");
        }
        if (System.getProperty("MinimumActionsForSession")!=null){
            try {
                minimumActionsForSession = Long.parseLong(System.getProperty("MinimumActionsForSession"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Takes the complete input as String, splits into multiple entries and processes each
     */
    public void process(String inputString) {
        DynatraceVisit dynatraceVisit = Parser.parseVisit(inputString);
        processSession(dynatraceVisit);
    }

    private void processSession(DynatraceVisit visit) {
        if (inputFilter.isCorrectApplication(visit)){
            ProbitSession probitSession = new ProbitSession();
            probitSession.setType(visit.getApplication());

            List<ProbitAction> probitActions = new ArrayList<>();
            for (DynatraceAction action: visit.getActions()) {
                if (!inputFilter.isActionToFilter(action)){
                    ProbitAction probitAction = new ProbitAction();
                    probitAction.setName(action.getActionPrettyName());
                    List<ProbitParameter> probitParameters = new ArrayList<>();
                    probitParameters.add(new ProbitParameter("prettyName", action.getActionPrettyName()));
                    probitParameters.add(new ProbitParameter("target", action.getTargetUrl()));
                    probitParameters.add(new ProbitParameter("type", action.getType()));
                    probitParameters.add(new ProbitParameter("startTime", action.getStartTime().toString()));
                    probitParameters.add(new ProbitParameter("endTime", action.getEndTime().toString()));
                    probitAction.setParameters(probitParameters);
                    preprocessAction(probitAction);
                    probitActions.add(probitAction);
                }
            }

            probitSession.setActions(probitActions);
            send(probitSession);
         }
    }

    public void preprocessAction(ProbitAction probitAction) {
        if (probitAction.getName().matches("Loading of page /nl.*")){
            probitAction.setName(probitAction.getName().replace("/nl", "/<Lang>"));
            probitAction.getParameters().add(new ProbitParameter("Language", "nl"));
        }
        if (probitAction.getName().matches("Loading of page /fr.*")){
            probitAction.setName(probitAction.getName().replace("/fr", "/<Lang>"));
            probitAction.getParameters().add(new ProbitParameter("Language", "fr"));
        }
        if (probitAction.getName().matches(".*/hotel/.*")){
            String[] stringParts = probitAction.getName().split("/");
            if (stringParts.length>5) {
                probitAction.getParameters().add(new ProbitParameter("Country", stringParts[3]));
                probitAction.getParameters().add(new ProbitParameter("Region", stringParts[4]));
                probitAction.getParameters().add(new ProbitParameter("Place", stringParts[5]));
                probitAction.setName(stringParts[0] + "/"+ stringParts[1] +"/"+ stringParts[2] + "/<Country>/<Region>/<Place>");
            }
        }

    }

    private void send(ProbitSession session) {
        try {
            RestTemplate rest = new RestTemplate();
            rest.getInterceptors().add(
                new BasicAuthorizationInterceptor(user, password));
            Object response = rest.postForObject(endPoint, this.getJsonEntity(session), Object.class);
            LOGGER.debug("response {}", response);
        } catch (Exception e) {
            LOGGER.error("Can't send session", e);
        }
    }

    private HttpEntity<String> getJsonEntity(ProbitSession session) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(this.toJson(session), headers);
    }

    private String toJson(ProbitSession session) {
        String json = GSON.toJson(session);
        LOGGER.debug("session json -> {}", json);
        return json;
    }


}


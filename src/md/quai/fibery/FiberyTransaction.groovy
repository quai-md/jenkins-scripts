package md.quai.fibery

import com.nu.art.core.tools.StreamTools
import com.nu.art.http.Transaction_JSON
import com.nu.art.http.consts.HttpMethod
import com.nu.art.pipeline.exceptions.BadImplementationException
import groovy.json.JsonSlurper

import java.util.Arrays
import java.util.HashMap
import java.util.List

public class FiberyTransaction extends Transaction_JSON {

    //######################## Params ########################

    private final HashMap<String, List<String>> allowedStates;
    private final HashMap<String, String> promoteToState;
    private final String token;

    //######################## Init ########################

    public FiberyTransaction(String token) {
        this.token = token;
        this.allowedStates = new HashMap<String, List<String>>();
        this.promoteToState = new HashMap<String, String>();

        List<String> devAllowedIds = Arrays.asList(FiberyModule.readyForDevId, FiberyModule.inProgressId);
        List<String> stgAllowedIds = Arrays.asList(FiberyModule.devDoneId);
        List<String> prodAllowedIds = Arrays.asList(FiberyModule.validatedInStg);

        allowedStates.put("DEV", devAllowedIds);
        allowedStates.put("STG", stgAllowedIds);
        allowedStates.put("PROD", prodAllowedIds);

        promoteToState.put("DEV", FiberyModule.devDoneId);
        promoteToState.put("STG", FiberyModule.toValidateInStg);
        promoteToState.put("PROD", FiberyModule.toValidateInProd);
    }

    //######################## Functionality ########################

    public void queryTasks(String[] taskPublicIds) {
        this.logInfo('############# HERE - queryTasks #############')
        def queryBody = [[
                                 command: "fibery.entity/query",
                                 args   : [
                                         query : [
                                                 "q/from"  : "Main/Task",
                                                 "q/where" : ["q/in", "fibery/public-id", "\$publicIds"],
                                                 "q/limit" : "q/no-limit",
                                                 "q/select": ["fibery/id", "fibery/public-id", ["workflow/state": ["enum/name", "fibery/id"]]]
                                         ],
                                         params: [
                                                 $publicIds: taskPublicIds
                                         ]
                                 ]
                         ]]
        String URL = "https://quai.fibery.io/api/commands";
        def stream = createRequest()
                .setMethod(HttpMethod.Post)
                .setUrl(URL)
                .addHeader("Authorization", "Token " + token)
                .setBody(gson.toJson(queryBody))
                .executeSync()

        def responseAsString = StreamTools.readFullyAsString(stream);

        this.logInfo("############# HERE - response as string #############")
        this.logInfo(responseAsString);

        this.logInfo("############# HERE - Task Validation #############");

        String env = "DEV"
        def data = new JsonSlurper().parseText(responseAsString);
        List<String> nonPassingTaskIds = new ArrayList<String>();

        data.each { query ->
            query.result.each { result ->
                if (!validateTaskState(result, env))
                    nonPassingTaskIds.add(result["fibery/public-id"]);
            }
        }

        if (nonPassingTaskIds.size() > 0) {
            String error = String.format("Tasks with invalid states: %s", String.join(",", nonPassingTaskIds));
            throw new BadImplementationException(error);
        }

        this.logInfo("Task Validation - All tasks are okay!");

        this.logInfo("############# HERE - Task Promotion #############");
        def updateBody = [];

        data.each { query ->
            query.result.each { result ->
                updateBody.add(generateTaskPromotionQuery(result, env))
            }
        }

        def updateStream = createRequest()
                .setMethod(HttpMethod.Post)
                .setUrl(URL)
                .addHeader("Authorization", "Token " + token)
                .setBody(gson.toJson(updateBody))
                .executeSync()

        def updateResponseAsString = StreamTools.readFullyAsString(updateStream);
        this.logInfo("############# HERE - update response as string #############")
        this.logInfo(updateResponseAsString);

    }

    private boolean validateTaskState(def task, String env) {
        String taskStateId = task["workflow/state"]["fibery/id"];
        return this.allowedStates.get(env).contains(taskStateId);
    }

    private LinkedHashMap<String, Object> generateTaskPromotionQuery(def task, String env) {
        String promoteId = this.promoteToState.get(env);
        return [
                command: "fibery.entity/update",
                args   : [
                        "type"  : "Main/Task",
                        "entity": [
                                "fibery/id"     : task["fibery/id"],
                                "workflow/state": ["fibery/id": promoteId]
                        ]
                ]
        ]
    }
//
//    private void promoteTasks(FiberyQueryResponse_Result[] tasks, String env) {
//        this.logInfo("Promoting Tasks");
//        List<FiberyUpdateParams> commands = new ArrayList<>();
//        String URL = "https://quai.fibery.io/api/commands";
//        String promoteToId = this.promoteToState.get(env);
//
//        for (FiberyQueryResponse_Result task : tasks) {
//            commands.add(new FiberyUpdateParams(task.id, promoteToId));
//        }
//
//        createRequest()
//                .setMethod(HttpMethod.Post)
//                .setUrl(URL)
//                .addHeader("Authorization", "Token " + token)
//                .setBody(gson.toJson(commands))
//                .execute(new JsonHttpResponseListener(HashMap.class) {
//                    @Override
//                    public void onSuccess(HttpResponse httpResponse, HashMap[] responseBody) {
//                        FiberyTransaction.this.onTasksPromoted(responseBody);
//                    }
//
//                    @Override
//                    public void onError(HttpResponse httpResponse, String errorAsString) {
//                        System.out.println(httpResponse.responseCode);
//                    }
//                });
//    }
}

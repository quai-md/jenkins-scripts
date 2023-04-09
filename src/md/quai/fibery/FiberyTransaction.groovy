package md.quai.fibery

import com.google.gson.annotations.SerializedName
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

    //######################## Query Classes ########################

//    private Object[] getSelect() {
//        HashMap toRet = new HashMap();
//        toRet.put(1, "fibery/id");
//        toRet.put(2, "fibery/public-id");
//        toRet.put(3, new WorkflowState());
//        return toRet.values().toArray();
//    }
//
//    public class WorkflowState {
//        @SerializedName("workflow/state")
//        String[] state = ["enum/name", "fibery/id"];
//    }
//
//    public class Query {
//        @SerializedName("q/from")
//        String from = "Main/Task";
//        @SerializedName("q/where")
//        String[] where = ["q/in", "fibery/public-id", "$publicIds"];
//        @SerializedName("q/limit")
//        String limit = "q/no-limit";
//        @SerializedName("q/select")
//        Object[] select = getSelect();
//    }
//
//    public class Params {
//        @SerializedName("$publicIds")
//        String[] publicIds;
//
//        public Params(String[] ids) {
//            this.publicIds = ids;
//        }
//    }
//
//    public class FiberyQueryArgs {
//        Query query;
//        Params params;
//
//        public FiberyQueryArgs(String[] ids) {
//            this.query = new Query();
//            this.params = new Params(ids);
//        }
//    }
//
//    public class FiberyQueryParams {
//        public String command = "fibery.entity/query";
//        public Object args;
//
//        public FiberyQueryParams(String[] ids) {
//            this.args = new FiberyQueryArgs(ids);
//        }
//    }

    //######################## Update Classes ########################

    public class FiberyUpdateParams {
        public String command = "fibery.entity/update";
        public FiberyUpdateArgs args;

        public FiberyUpdateParams(String taskId, String promoteToId) {
            this.args = new FiberyUpdateArgs(taskId, promoteToId);
        }
    }

    public class FiberyUpdateArgs {
        String type = "Main/Task";
        FiberyUpdateArgs_Entity entity;

        public FiberyUpdateArgs(String taskId, String promoteToId) {
            this.entity = new FiberyUpdateArgs_Entity(taskId, promoteToId);
        }
    }

    public class FiberyUpdateArgs_Entity {
        @SerializedName("fibery/id")
        String taskId;
        @SerializedName("workflow/state")
        FiberyUpdateArgs_WorkflowState state;

        public FiberyUpdateArgs_Entity(String taskId, String promoteToId) {
            this.taskId = taskId;
            this.state = new FiberyUpdateArgs_WorkflowState(promoteToId);
        }
    }

    public class FiberyUpdateArgs_WorkflowState {
        @SerializedName("fibery/id")
        String id;

        public FiberyUpdateArgs_WorkflowState(String id) {
            this.id = id;
        }
    }

    //######################## Functionality ########################

    public void queryTasks(String[] taskPublicIds) {
        this.logInfo('############# HERE - queryTasks #############')
        def body = [[
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
                .setBody(gson.toJson(body))
                .executeSync()

        def responseAsString = StreamTools.readFullyAsString(stream);

        this.logInfo("############# HERE - response as string #############")
        this.logInfo(responseAsString);

        this.logInfo("############# HERE - Task Validation #############");

        def data = new JsonSlurper().parseText(responseAsString);
        List<String> nonPassingTaskIds = new ArrayList<String>();

        data.each { query ->
            query.result.each { result ->
                if (!validateTaskState(result, "DEV"))
                    nonPassingTaskIds.add(result["fibery/public-id"]);
            }
        }

        if (nonPassingTaskIds.size() > 0) {
            String error = String.format("Tasks with invalid states: %s", String.join(",", nonPassingTaskIds));
            throw new BadImplementationException(error);
        }

        this.logInfo("Task Validation - All tasks are okay!");
    }

//    private void handleQueryOnSuccess(FiberyQueryResponse_Result[] tasks) {
//        String env = "DEV";
//        this.logInfo("Deploying branch" + env);
//        this.validateTaskStates(tasks, env);
//        this.promoteTasks(tasks, env);
//    }
//
    private boolean validateTaskState(def task, String env) {
        String taskStateId = task["workflow/state"]["fibery/id"];
        return this.allowedStates.get(env).contains(taskStateId);
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

//    private void onTasksPromoted(FiberyUpdateResponse[] response) {
//        this.logInfo("Tasks Promoted");
//        System.out.println(gson.toJson(response));
//    }
}

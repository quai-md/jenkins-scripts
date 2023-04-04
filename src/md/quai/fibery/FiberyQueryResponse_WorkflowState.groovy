import com.google.gson.annotations.SerializedName

@Grab('com.google.code.gson:gson:2.8.6')

class FiberyQueryResponse_WorkflowState {
    @SerializedName("fibery/id")
    String id;
    @SerializedName("enum/name")
    String name;
}
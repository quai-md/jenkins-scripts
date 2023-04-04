package md.quai.fibery;

import com.google.gson.annotations.SerializedName;

public class FiberyUpdateResponse {
    static class FiberyUpdateResponse_Result {
        @SerializedName("fibery/id")
        String id;
    }
    boolean success;
    FiberyUpdateResponse_Result result;
}

package software.amazon.redshiftserverless.namespace;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    boolean callBackForDelete = false;
    public void setCallBackForDelete(boolean callBackForDelete) {
        this.callBackForDelete = callBackForDelete;
    }

    public boolean getCallBackForDelete() {
        return callBackForDelete;
    }
}

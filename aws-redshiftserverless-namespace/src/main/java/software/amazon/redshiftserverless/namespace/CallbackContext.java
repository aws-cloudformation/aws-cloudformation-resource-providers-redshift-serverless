package software.amazon.redshiftserverless.namespace;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    String namespaceArn = null;
    boolean callBackForDelete = false;
    String adminPasswordSecretArn = null;

    public void setNamespaceArn(String namespaceArn) {this.namespaceArn = namespaceArn; }

    public String getNamespaceArn() { return namespaceArn; }

    public void setCallBackForDelete(boolean callBackForDelete) {
        this.callBackForDelete = callBackForDelete;
    }

    public boolean getCallBackForDelete() {
        return callBackForDelete;
    }

    public String getAdminPasswordSecretArn() {
        return adminPasswordSecretArn;
    }

    public void setAdminPasswordSecretArn(String adminPasswordSecretArn) {
        this.adminPasswordSecretArn = adminPasswordSecretArn;
    }
}

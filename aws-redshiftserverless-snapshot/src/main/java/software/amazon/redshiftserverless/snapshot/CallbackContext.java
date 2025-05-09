package software.amazon.redshiftserverless.snapshot;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    int retryOnResourceNotFound = 5;
    boolean propagationDelay = true;
    String namespaceName;

    public void setNamespaceName(String namespaceName) {this.namespaceName = namespaceName; }

    public String getNamespaceName() { return namespaceName; }
}

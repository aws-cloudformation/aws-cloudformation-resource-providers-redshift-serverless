package software.amazon.redshiftserverless.namespace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.redshiftserverless.model.TagResourceRequest;
import software.amazon.awssdk.services.redshiftserverless.model.UntagResourceRequest;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTagsRequest {
    private TagResourceRequest createNewTagsRequest;
    private UntagResourceRequest deleteOldTagsRequest;
}

package org.elasticsearch.rest.action.unix;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;

/**
 * @drewr
 */
public class RestUnixShardsAction extends BaseRestHandler {

    @Inject
    public RestUnixShardsAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/_unix/shards", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        RestStatus status = RestStatus.OK;
        try {
            channel.sendResponse(new StringRestResponse(status, "shard1\nshard2"));
        } catch (Throwable e) {
            try {
                channel.sendResponse(new XContentThrowableRestResponse(request, e));

            } catch (Exception e1) {
                logger.warn("Failed to send response", e);
            }
        }
    }
}

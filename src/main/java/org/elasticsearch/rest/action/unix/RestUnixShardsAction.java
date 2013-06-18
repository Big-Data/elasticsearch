package org.elasticsearch.rest.action.unix;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;
import java.util.Iterator;

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
        ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.listenerThreaded(false);
        clusterStateRequest.filterMetaData(true);
        clusterStateRequest.local(true);
        client.admin().cluster().state(clusterStateRequest, new ActionListener<ClusterStateResponse>() {
            @Override
            public void onResponse(ClusterStateResponse clusterStateResponse) {
                RestStatus status = RestStatus.OK;
                try {
                    StringBuilder out = new StringBuilder();
                    for (Iterator<ShardRouting> it = clusterStateResponse.getState().getRoutingTable().allShards().iterator(); it.hasNext(); ) {
                        ShardRouting shard = it.next();
                        out.append(shard.index());
                        out.append(" ");
                        out.append(shard.id());
                        out.append(" ");
                        out.append(shard.primary());
                        out.append("\n");
                    }
                    channel.sendResponse(new StringRestResponse(status, out.toString().trim()));
                } catch (Throwable e) {
                    try {
                        channel.sendResponse(new XContentThrowableRestResponse(request, e));

                    } catch (Exception e1) {
                        logger.warn("Failed to send response", e);
                    }
                }
            }

            @Override
            public void onFailure(Throwable e) {
                logger.error("wtf", e);
            }
        });
    }
}

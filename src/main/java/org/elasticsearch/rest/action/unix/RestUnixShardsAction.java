package org.elasticsearch.rest.action.unix;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.admin.indices.status.ShardStatus;
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
        final StringBuilder out = new StringBuilder();
        IndicesStatusRequest indicesStatusRequest = new IndicesStatusRequest();
        client.admin().indices().status(indicesStatusRequest, new ActionListener<IndicesStatusResponse>() {
            @Override
            public void onResponse(IndicesStatusResponse indicesStatusResponse) {
                RestStatus status = RestStatus.OK;
                for (ShardStatus shard : indicesStatusResponse.getShards()) {
                    String pri = "r";
                    if (shard.getShardRouting().primary()) {
                        pri = "p";
                    }
                    out.append(shard.getIndex());
                    out.append(" ");
                    out.append(shard.getState().id());
                    out.append(" ");
                    out.append(pri);
                    out.append(" ");
                    out.append(shard.getShardRouting().state());
                    out.append(" ");
                    out.append(shard.getDocs().getNumDocs());
                    out.append(" ");
                    out.append(shard.getStoreSize().toString());
                    out.append(" ");
                    out.append(shard.getStoreSize().bytes());
                    out.append("\n");

                }
                channel.sendResponse(new StringRestResponse(status, out.toString().trim()));

            }

            @Override
            public void onFailure(Throwable e) {
                logger.error("wtf", e);
            }
        });
    }
}

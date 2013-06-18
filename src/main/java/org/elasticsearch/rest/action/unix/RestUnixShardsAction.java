package org.elasticsearch.rest.action.unix;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.admin.indices.status.ShardStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.google.common.collect.Maps.newHashMap;
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

        final Map<String, RoutingNode> nodes = newHashMap();

        ClusterStateRequest clusterStateRequest = new ClusterStateRequest();                                                                                                     clusterStateRequest.listenerThreaded(false);
        clusterStateRequest.filterMetaData(true);
        clusterStateRequest.local(true);

        final CountDownLatch latch = new CountDownLatch(1);

        client.admin().cluster().state(clusterStateRequest, new ActionListener<ClusterStateResponse>() {
            @Override
            public void onResponse(ClusterStateResponse clusterStateResponse) {
                Map<String, RoutingNode> m = ImmutableMap.copyOf(clusterStateResponse.getState().routingNodes().nodesToShards());

                for (Map.Entry<String,RoutingNode> e : m.entrySet()) {
                    nodes.put(e.getKey(), e.getValue());
                }

                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                logger.warn("wtf", e);
            }
        });

        try {
            latch.await();
        } catch (Exception e) {
            logger.warn("not getting response from cluster state request", e);
        }

        IndicesStatusRequest indicesStatusRequest = new IndicesStatusRequest();
        client.admin().indices().status(indicesStatusRequest, new ActionListener<IndicesStatusResponse>() {
            @Override
            public void onResponse(IndicesStatusResponse indicesStatusResponse) {
                try {
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
                        out.append(" ");
                        if (null != nodes.get(shard.getShardRouting().currentNodeId())) {
                            InetSocketTransportAddress addr = (InetSocketTransportAddress) nodes.get(shard.getShardRouting().currentNodeId()).node().address();
                            out.append(addr.address().getHostName());
                        }
                        out.append("\n");

                    }
                    channel.sendResponse(new StringRestResponse(status, out.toString().trim()));

                } catch (Throwable e) {
                    channel.sendResponse(new StringRestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.toString()));
                }

            }

            @Override
            public void onFailure(Throwable e) {
                logger.error("wtf", e);
            }
        });
    }
}

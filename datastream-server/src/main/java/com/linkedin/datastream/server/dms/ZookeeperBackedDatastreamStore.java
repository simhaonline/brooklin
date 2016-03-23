package com.linkedin.datastream.server.dms;

import com.linkedin.datastream.common.Datastream;
import com.linkedin.datastream.common.DatastreamException;
import com.linkedin.datastream.common.DatastreamUtils;
import com.linkedin.datastream.common.zk.ZkClient;
import com.linkedin.datastream.server.zk.KeyBuilder;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ZookeeperBackedDatastreamStore implements DatastreamStore {

  private static final Logger LOG = LoggerFactory.getLogger(ZookeeperBackedDatastreamStore.class.getName());

  private final ZkClient _zkClient;
  private final String _rootPath;
  // Cache of the datastream key list
  private volatile List<String> _datastreamList = Collections.emptyList();

  public ZookeeperBackedDatastreamStore(ZkClient zkClient, String cluster) {
    assert zkClient != null;
    assert cluster != null;

    _zkClient = zkClient;
    _rootPath = KeyBuilder.datastreams(cluster);

    // Be notified of changes to the children list in order to cache it. The listener creates a copy of the list
    // because other listeners could potentially get access to it and modify its contents.
    _zkClient.subscribeChildChanges(_rootPath, (parentPath, currentChildren) ->
        _datastreamList = currentChildren.stream().collect(Collectors.toCollection(() -> new ArrayList<>(currentChildren.size()))));
  }

  private String getZnodePath(String key) {
    return _rootPath + "/" + key;
  }

  @Override
  public Datastream getDatastream(String key) {
    if (key == null) {
      return null;
    }
    String path = getZnodePath(key);
    String json = _zkClient.readData(path, true /* returnNullIfPathNotExists */);
    if (json == null) {
      return null;
    }
    return DatastreamUtils.fromJSON(json);
  }

  /**
   * Retrieves all the datastreams in the store. Since there may be many datastreams, it is better
   * to return a Stream and enable further filtering and transformation rather that just a List.
   *
   * The datastream key-set used to make this call is cached, it is possible to get a slightly outdated
   * list of datastreams and not have a stream that was just added. It depends on how long it takes for
   * ZooKeeper to notify the change.
   *
   * @return
   */
  @Override
  public Stream<String> getAllDatastreams() {
    return _datastreamList.stream().sorted();
  }

  @Override
  public void updateDatastream(String key, Datastream datastream) throws DatastreamException {
    // Updating a Datastream is still tricky for now. Changing either the
    // the source or target may result in failure on connector.
    // We could possibly only allow updates on metadata field
  }

  @Override
  public void createDatastream(String key, Datastream datastream) throws DatastreamException {
    Validate.notNull(datastream, "null datastream");
    Validate.notNull(key, "null key for datastream" + datastream);

    String path = getZnodePath(key);
    if (_zkClient.exists(path)) {
      String content = _zkClient.ensureReadData(path);
      String errorMessage = String.format("Datastream already exists: path=%s, content=%s", key, content);
      LOG.warn(errorMessage);
      throw new DatastreamException(errorMessage);
    }
    _zkClient.ensurePath(path);
    String json = DatastreamUtils.toJSON(datastream);
    _zkClient.writeData(path, json);
  }

  @Override
  public void deleteDatastream(String key) {
    Validate.notNull(key, "null key");
    String path = getZnodePath(key);
    if (_zkClient.exists(path)) {
      _zkClient.delete(getZnodePath(key));
    }
  }
}

package org.opentripplanner.plugin;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PluginManager {

    private static Logger LOG = LoggerFactory.getLogger(PluginManager.class);

    private Multimap<Class<?>, Pluggable> subscribers = ArrayListMultimap.create();

    /**
     * Subscribe a plugin to a type of message
     * @param messageType class of message which plugin should be subscribed to
     * @param plugin plugin to subscribe
     */
    public void subscribe(Class<?> messageType, Pluggable plugin) {
        subscribers.put(messageType, plugin);
    }

    /**
     * Hook plugins into OTP code, via publishing a message.
     * @param message Message to publish
     * @return responses of all plugins subscribed to this message.
     */
    public List<Object> publish(Object message) {
        List<Object> responses = new ArrayList<>();
        for (Pluggable plugin : subscribers.get(message.getClass())) {
            Object response = plugin.receive(message);
            if (response != null) {
                responses.add(response);
            }
        }
        return responses;
    }

    /**
     * Get a plugin for a particular type.
     * @param klass Subscription type for plugin
     * @return Plugin which matches that type. If there are multiple plugins, return the first.
     */
    public <T> T getPluginByType(Class<T> klass) {
        Collection<Pluggable> plugins = subscribers.get(klass);
        for (Pluggable plugin : plugins) {
            if (klass.isInstance(plugin)) {
                return klass.cast(plugin);
            }
        }
        return null;
    }
}

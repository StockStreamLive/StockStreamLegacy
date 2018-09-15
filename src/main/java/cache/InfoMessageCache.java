package cache;


import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import data.InfoMessage;
import data.OrderResult;
import data.factory.InfoMessageFactory;
import logic.PubSub;
import logic.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import utils.TextUtil;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InfoMessageCache {
    @Autowired
    private InfoMessageFactory infoMessageFactory;

    @Autowired
    private MetricRegistry metricsRegistry;

    @Autowired
    private PubSub pubSub;

    @Autowired
    private Scheduler scheduler;

    private Map<String, InfoMessage> idToInfo = new ConcurrentHashMap<>();
    private Set<String> storedURLs = Collections.synchronizedSet(new HashSet<>());
    private List<InfoMessage> messages = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentLinkedQueue<InfoMessage> priorityInfoMessages = new ConcurrentLinkedQueue<>();

    private int iterator = 0;
    private int idCount = 0;

    @PostConstruct
    public void init() {
        pubSub.subscribe(this::addMessage, InfoMessage.class);
        pubSub.subscribe(this::addOrderResult, OrderResult.class);
        scheduler.scheduleJob(this::resort, 20L, 300L, TimeUnit.SECONDS);
    }

    public synchronized Optional<String> getURLForId(final String id) {
        return Optional.ofNullable(idToInfo.get(id).getUrl());
    }

    public synchronized Map<String, InfoMessage> getNext() {
        if (messages.size() == 0) {
            return Collections.emptyMap();
        }

        if (priorityInfoMessages.size() > 0) {
            InfoMessage priorityMessage = priorityInfoMessages.poll();
            idToInfo.put("!last", priorityMessage);
            return ImmutableMap.of("!last", priorityMessage);
        }

        if (iterator > messages.size()) {
            iterator = 0;
        }

        final InfoMessage nextMessage = messages.get(iterator++);
        final String messageId = String.format("!%s", idCount);
        idToInfo.put(messageId, nextMessage);
        idCount++;
        if (idCount > 99) {
            idCount = 0;
        }

        return ImmutableMap.of(messageId, nextMessage);
    }

    private synchronized Void addOrderResult(final OrderResult orderResult) {
        InfoMessage infoMessage = this.infoMessageFactory.constructInfoMessageFromOrderResult(orderResult);
        priorityInfoMessages.add(infoMessage);
        return null;
    }

    private synchronized void resort() {
        log.info("Shuffling and filtering {} InfoMessages", messages.size());
        final List<InfoMessage> messageList = Collections.synchronizedList(new ArrayList<>());
        final Queue<InfoMessage> queue = new ConcurrentLinkedQueue<>(messages);

        final long now = new Date().getTime();

        while (!queue.isEmpty()) {
            InfoMessage message = queue.poll();
            if (message == null) {
                continue;
            }

            final long msAgo = now - message.getTimestamp();

            if (msAgo > 2 * 3600000) {
                continue;
            }

            messageList.add(message);
        }

        Collections.shuffle(messageList);
        messages = messageList;

        log.info("Have {} left", messageList.size());

        iterator = 0;
    }

    private String sanitizeText(final String inputText) {
        return TextUtil.replaceNonPrintableText(TextUtil.stripURLS(inputText));
    }

    private synchronized Void addMessage(final InfoMessage infoMessage) {
        if (infoMessage.getText().length() <= 45 || infoMessage.getText().length() > 375) {
            return null;
        }
        if (storedURLs.contains(infoMessage.getUrl())) {
            return null;
        }

        final Meter requests = this.metricsRegistry.meter(infoMessage.getPlatform());
        requests.mark();

        infoMessage.setText(sanitizeText(infoMessage.getText()));
        infoMessage.setSender(sanitizeText(infoMessage.getSender()));
        messages.add(infoMessage);
        storedURLs.add(infoMessage.getUrl());
        return null;
    }

}

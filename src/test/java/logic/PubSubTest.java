package logic;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class PubSubTest {

    @Mock
    private MetricRegistry metricRegistry;

    @InjectMocks
    private PubSub pubSub;

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);

        final Timer mockTimer = Mockito.mock(Timer.class);
        when(mockTimer.time()).thenReturn(Mockito.mock(Timer.Context.class));
        when(metricRegistry.timer(anyString())).thenReturn(mockTimer);
    }

    @Test
    public void testPublish_functionSubscribed_expectRunnableCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribe((Function<String, Void>) str -> {
            assertEquals(str, "testing123");
            applyMethodCalled.setTrue();
            return null;
        }, String.class);

        pubSub.publish(String.class, "testing123");

        assertEquals(true, applyMethodCalled.booleanValue());
    }

    @Test
    public void testPublish_functionSubscribedDifferentMessage_expectRunnableNotCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribe((Function<Integer, Void>) integ -> {
            applyMethodCalled.setTrue();
            return null;
        }, Integer.class);

        pubSub.publish(String.class, "testing123");

        assertEquals(false, applyMethodCalled.booleanValue());
    }

    @Test
    public void testPublish_runnableSubscribed_expectRunnableCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribeRunnable(applyMethodCalled::setTrue, String.class);

        pubSub.publish(String.class, "testing123");

        assertEquals(true, applyMethodCalled.booleanValue());
    }

    @Test
    public void testPublish_runnableSubscribedDifferentMessage_expectRunnableNotCalled() {
        final MutableBoolean applyMethodCalled = new MutableBoolean(false);

        pubSub.subscribeRunnable(applyMethodCalled::setTrue, Integer.class);

        pubSub.publish(String.class, "testing123");

        assertEquals(false, applyMethodCalled.booleanValue());
    }

}

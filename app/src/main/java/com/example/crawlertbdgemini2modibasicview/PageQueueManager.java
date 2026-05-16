package com.example.crawlertbdgemini2modibasicview;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PageQueueManager {

    public static final BlockingQueue<Integer> PAGE_QUEUE =
            new LinkedBlockingQueue<>(100);

}

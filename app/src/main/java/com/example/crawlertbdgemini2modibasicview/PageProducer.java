package com.example.crawlertbdgemini2modibasicview;

public class PageProducer extends Thread {
    private final DBHelperThuoc dbHelper;
    public PageProducer(DBHelperThuoc dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public void run() {

        int page = dbHelper.getCurrentPage();

        while (!GlobalState.STOP.get()) {

            try {

                // Tránh queue quá lớn
                if (PageQueueManager.PAGE_QUEUE.size() < 50) {

                    PageQueueManager.PAGE_QUEUE.put(page);

                    System.out.println(
                            "ADD PAGE: " + page);
                    // checkpoint
                    if (page % 5 == 0) {
                        dbHelper.saveCurrentPage(page);
                    }

                    page++;
                }

                Thread.sleep(50);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Producer stopped");
    }
}

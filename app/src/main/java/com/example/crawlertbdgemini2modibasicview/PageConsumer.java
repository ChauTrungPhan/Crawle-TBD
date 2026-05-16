package com.example.crawlertbdgemini2modibasicview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageConsumer extends Thread {

    private static final String BASE_URL =
            "https://thuocbietduoc.com.vn/thuoc/drgsearch.aspx?page=";
    private final DBHelperThuoc dbHelper;

    public PageConsumer(DBHelperThuoc dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public void run() {

        while (!GlobalState.STOP.get()) {

            try {

                Integer page =
                        PageQueueManager.PAGE_QUEUE.take();

                crawlPage(page);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        System.out.println(
//                Thread.currentThread().getName()
//                        + " stopped"
//        );
    }

    private void crawlPage(int page) {

        try {
            String url = BASE_URL + page;

            System.out.println(
                    Thread.currentThread().getName()
                            + " crawling page "
                            + page
            );

            Document doc =
                    Jsoup.connect(url)
                            .userAgent("Mozilla/5.0")
                            .timeout(30000)
                            .get();

            Elements links =
                    doc.select("a[href*=thuoc-]");

            // Không có thuốc
            if (links.isEmpty()) {

                int empty =
                        GlobalState.EMPTY_PAGE_COUNT
                                .incrementAndGet();

                System.out.println(
                        "EMPTY PAGE: " + page
                );

                // 3 page rỗng liên tiếp
                if (empty >= 3) {

                    GlobalState.STOP.set(true);

                    System.out.println(
                            "STOP: many empty pages"
                    );
                }

                return;
            }

            // Reset empty count
            GlobalState.EMPTY_PAGE_COUNT.set(0);

            int newDrugCount = 0;

            for (Element a : links) {

                String href =
                        a.attr("href");

                String maThuoc =
                        extractMaThuoc(href);

                if (maThuoc == null)
                    continue;

                // Chống trùng
                if (GlobalState.DRUG_MAP
                        .putIfAbsent(maThuoc, true)
                        == null) {

                    newDrugCount++;

                    String detailUrl;

                    if (href.startsWith("http")) {

                        detailUrl = href;

                    } else {

                        detailUrl =
                                "https://thuocbietduoc.com.vn"
                                        + href;
                    }

                    System.out.println(
                            "NEW DRUG: "
                                    + maThuoc
                    );

                    // TODO:
                    // saveSQLite(maThuoc, detailUrl);

                }
            }

            System.out.println(
                    "PAGE "
                            + page
                            + " new drugs = "
                            + newDrugCount
            );

            // Không có thuốc mới
            if (newDrugCount == 0) {

                int duplicate =
                        GlobalState.DUPLICATE_PAGE_COUNT
                                .incrementAndGet();

                // Có thể đã loop page cuối
                if (duplicate >= 5) {

                    GlobalState.STOP.set(true);

                    System.out.println(
                            "STOP: duplicate pages"
                    );
                }

            } else {

                GlobalState.DUPLICATE_PAGE_COUNT
                        .set(0);
            }

            Thread.sleep(300);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private String extractMaThuoc(String href) {

        try {

            Matcher m =
                    Pattern.compile(
                            "thuoc-([a-zA-Z0-9\\-_]+)"
                    ).matcher(href);

            if (m.find()) {

                return m.group(1)
                        .toLowerCase();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}

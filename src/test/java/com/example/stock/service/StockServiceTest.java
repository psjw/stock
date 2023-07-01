package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockServiceTest {
    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before() {
        Stock stock = new Stock(1L, 100L);

        stockRepository.saveAndFlush(stock);
    }


    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void stock_decrease() {
        stockService.decrease(1L, 1L);

        //100 - 1 = 99
        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(99, stock.getQuantity());
    }

    @Test
    public void 동시에_100개의_요청() throws InterruptedException {
        //Runtime.getRuntime().availableProcessors()는 현재 사용가능한 core 개수 리턴
//        final int maxCore = Runtime.getRuntime().availableProcessors();
//        System.out.println("MaxCore : " + maxCore);
        //ExecutorService : 비동기를 실행하는 작업을 단순하게 수행할수 있도록 도와줌
        /*
            newFixedThreadPool(int) : 인자 개수만큼 고정된 쓰레드풀을 만듭니다.
            newCachedThreadPool(): 필요할 때, 필요한 만큼 쓰레드풀을 생성합니다. 이미 생성된 쓰레드를 재활용할 수 있기 때문에 성능상의 이점이 있을 수 있습니다.
            newScheduledThreadPool(int): 일정 시간 뒤에 실행되는 작업이나, 주기적으로 수행되는 작업이 있다면 ScheduledThreadPool을 고려해볼 수 있습니다.
            newSingleThreadExecutor(): 쓰레드 1개인 ExecutorService를 리턴합니다. 싱글 쓰레드에서 동작해야 하는 작업을 처리할 때 사용합니다.
        */
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        //스레드에서 수행되고 있는 작업이 완료될때까지 기다려줌
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for(int i =0 ; i < threadCount ;i++){
            executorService.submit(()->{
                try{
                    stockService.decrease(1L,1L );
                }finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();
        //100 - (1*100) = 0
        assertEquals(0L, stock.getQuantity());

        //왜 실패인가?
        //Race Conditon  발생 -> 둘 이상의 스레드가 데이터를 Access를 할 수 있고, 동시에 변경을 하려고 할때 발생
        //예상
        // Thread-1                                             Stock                   Thread-2
        //select * from stock where id = 1                     {id: 1, quantity : 5}
        //update set quantity = 4 from stock where id = 1      {id: 1, quantity : 4}
        //                                                     {id: 1, quantity : 4}    select * from stock where id = 1
        //                                                     {id: 1, quantity : 3}    update set quantity = 3 from stock where id = 1

        //실제
        // Thread-1                                             Stock                   Thread-2
        //select * from stock where id = 1                     {id: 1, quantity : 5}
        //                                                     {id: 1, quantity : 5}    select * from stock where id = 1
        //update set quantity = 4 from stock where id = 1      {id: 1, quantity : 4}
        //                                                     {id: 1, quantity : 4}    update set quantity = 4 from stock where id = 1

    }
}
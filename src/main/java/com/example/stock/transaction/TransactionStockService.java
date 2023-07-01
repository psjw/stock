package com.example.stock.transaction;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.springframework.transaction.annotation.Transactional;

public class TransactionStockService {
    private StockRepository stockRepository;

    public TransactionStockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public synchronized void decrease(Long id, Long quantity){
        startTransaction();
        Stock stock = stockRepository.findById(id).orElseThrow();

        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);

        endTransaction();
    }

    private void startTransaction() {
    }

    private void endTransaction() {
    }
}

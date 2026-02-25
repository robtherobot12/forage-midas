package com.jpmc.midascore;

import com.jpmc.midascore.component.DatabaseConduit;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MidasCoreApplication {

    @Value("${general.incentive-url}")
    private String incentiveUrl;

    private final DatabaseConduit conduit;

    public MidasCoreApplication(DatabaseConduit conduit) {
        this.conduit = conduit;
    }

    public static void main(String[] args) {
        SpringApplication.run(MidasCoreApplication.class, args);
    }

    @KafkaListener(id="TransactionGroup", topics = "${general.kafka-topic}")
    public void listen(Transaction transaction) {
        UserRecord recipient = conduit.getUser(transaction.getRecipientId());
        UserRecord sender = conduit.getUser(transaction.getSenderId());
        if (recipient == null || sender == null) return;
        if(sender.getBalance() >= transaction.getAmount()) {
            RestTemplate rest = new RestTemplate();
            rest.setErrorHandler(new DefaultResponseErrorHandler());
            Incentive incentive;

            try {
                incentive = rest.postForObject(
                        incentiveUrl,
                        transaction,
                        Incentive.class
                );
            } catch (HttpStatusCodeException e) {
                System.err.println(e.getMessage());
                return;
            }

            TransactionRecord tR = new TransactionRecord(
                    sender.getId(),
                    recipient.getId(),
                    transaction.getAmount(),
                    incentive.getAmount()
            );
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive.getAmount());

            conduit.saveTransaction(tR);
            conduit.save(recipient);
            conduit.save(sender);
            System.out.println("Sender: " + sender.getName() + " Balance: " + sender.getBalance() + "\n"
            + "Recipient: " + recipient.getName() + " Balance: " + recipient.getBalance());
        }
    }

}

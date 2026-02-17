package be.codeforbelgium.openinzichten.service;

public interface MessageEncryptionService {
    String encrypt(String plain);

    String decrypt(String cipher);
}

package com.mmt.telegrambot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("🚀 Telegram SMS Bot Started Successfully!");
    }
}

@RestController
class HealthController {
    @GetMapping("/")
    public String health() {
        return "🤖 Telegram SMS Bot is Running! ✅";
    }
    
    @GetMapping("/status")
    public String status() {
        return "Bot Status: Active 🟢";
    }
}

@Component
class TelegramBotService {
    
    // تنظیمات ربات - اینجا رو با اطلاعات خودتون عوض کنید
    private static final String BOT_TOKEN = "8279388063:AAHKKo1TJkow5bT_tvpboIzWz1VQz_KcIwU";
    private static final String ADMIN_CHAT_ID = "6361426190";
    private static final String BOT_PASSWORD = "MMT2024";
    
    private int lastUpdateId = 0;
    private Map<String, Boolean> authenticatedUsers = new HashMap<>();
    private Map<String, String> userStates = new HashMap<>();
    private Map<String, Integer> wrongAttempts = new HashMap<>();
    
    @Scheduled(fixedDelay = 3000) // هر 3 ثانیه چک کن
    public void checkForMessages() {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastUpdateId + 1);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            processUpdates(response.toString());
        } catch (Exception e) {
            System.err.println("❌ Error checking messages: " + e.getMessage());
        }
    }
    
    private void processUpdates(String jsonResponse) {
        try {
            if (jsonResponse.contains("\"result\":[]")) {
                return;
            }

            // استخراج پیام‌ها
            String[] updates = jsonResponse.split("\"update_id\":");
            for (int i = 1; i < updates.length; i++) {
                try {
                    String update = updates[i];
                    
                    // استخراج update_id
                    String updateIdStr = update.split(",")[0].trim();
                    int updateId = Integer.parseInt(updateIdStr);
                    if (updateId > lastUpdateId) {
                        lastUpdateId = updateId;
                    }
                    
                    // استخراج chat_id
                    if (!update.contains("\"chat\":{\"id\":")) continue;
                    String chatIdPart = update.split("\"chat\":\\{\"id\":")[1];
                    String chatId = chatIdPart.split(",")[0];
                    
                    // استخراج متن
                    if (!update.contains("\"text\":\"")) continue;
                    String textPart = update.split("\"text\":\"")[1];
                    String text = textPart.split("\"")[0];
                    
                    // پردازش پیام
                    handleMessage(chatId, text);
                    
                } catch (Exception e) {
                    // ignore invalid updates
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing updates: " + e.getMessage());
        }
    }
    
    private void handleMessage(String chatId, String message) {
        try {
            System.out.println("📨 Message from " + chatId + ": " + message);
            
            // بررسی authentication
            if (!authenticatedUsers.getOrDefault(chatId, false)) {
                handleAuthentication(chatId, message);
                return;
            }
            
            // پردازش دستورات معمولی
            String response = "";
            String trimmedMessage = message.trim();
            
            if (trimmedMessage.equals("/start")) {
                response = "✅ شما قبلاً وارد شده‌اید!\n\n" +
                    "🤖 دستورات موجود:\n" +
                    "📱 /info - اطلاعات ربات\n" +
                    "❓ /help - راهنما\n" +
                    "🚪 /logout - خروج از حساب";
                    
            } else if (trimmedMessage.equals("/info")) {
                response = "🤖 ربات SMS تلگرام\n\n" +
                    "📡 وضعیت: فعال ✅\n" +
                    "🌐 سرور: Railway Cloud\n" +
                    "⚡ آپتایم: 24/7\n" +
                    "🔐 امنیت: محافظت شده";
                    
            } else if (trimmedMessage.equals("/help")) {
                response = "📋 راهنمای ربات:\n\n" +
                    "🔹 این ربات برای مدیریت پیامک‌ها طراحی شده\n" +
                    "🔹 تمام عملیات محرمانه و امن است\n" +
                    "🔹 دسترسی محدود به کاربران مجاز\n\n" +
                    "🆘 پشتیبانی: @YourSupport";
                    
            } else if (trimmedMessage.equals("/logout")) {
                authenticatedUsers.remove(chatId);
                userStates.remove(chatId);
                response = "🚪 با موفقیت از حساب خارج شدید!\n" +
                    "برای ورود مجدد /start بزنید.";
                    
            } else {
                response = "❓ دستور نامعتبر!\n\n" +
                    "دستورات موجود:\n" +
                    "📱 /info\n" +
                    "❓ /help\n" +
                    "🚪 /logout";
            }
            
            sendMessage(chatId, response);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling message: " + e.getMessage());
        }
    }
    
    private void handleAuthentication(String chatId, String message) {
        try {
            if (message.trim().equals("/start")) {
                sendMessage(chatId, "🔐 برای ورود به ربات، لطفاً پسورد را وارد کنید:");
                userStates.put(chatId, "waiting_password");
                return;
            }
            
            if (userStates.getOrDefault(chatId, "").equals("waiting_password")) {
                if (message.trim().equals(BOT_PASSWORD)) {
                    // پسورد صحیح
                    authenticatedUsers.put(chatId, true);
                    userStates.remove(chatId);
                    wrongAttempts.remove(chatId);
                    
                    String welcomeMessage = "✅ خوش آمدید!\n\n" +
                        "🎉 شما با موفقیت وارد ربات شدید!\n\n" +
                        "🤖 دستورات موجود:\n" +
                        "📱 /info - اطلاعات ربات\n" +
                        "❓ /help - راهنما\n" +
                        "🚪 /logout - خروج از حساب";
                    
                    sendMessage(chatId, welcomeMessage);
                    
                    // اطلاع به ادمین
                    if (!chatId.equals(ADMIN_CHAT_ID)) {
                        sendMessage(ADMIN_CHAT_ID, "🟢 کاربر جدید وارد شد!\n👤 Chat ID: " + chatId);
                    }
                    
                } else {
                    // پسورد اشتباه
                    int attempts = wrongAttempts.getOrDefault(chatId, 0) + 1;
                    wrongAttempts.put(chatId, attempts);
                    
                    if (attempts >= 3) {
                        // مسدود کردن
                        sendMessage(chatId, "🚫 شما بخاطر وارد کردن پسورد اشتباه مسدود شدید!\n\n" +
                            "⛔ دسترسی شما قطع شد.");
                        
                        // اطلاع به ادمین
                        sendMessage(ADMIN_CHAT_ID, "🚨 تلاش مشکوک!\n" +
                            "👤 Chat ID: " + chatId + "\n" +
                            "❌ 3 بار پسورد اشتباه وارد کرد");
                        return;
                    }
                    
                    sendMessage(chatId, "❌ پسورد اشتباه! (" + attempts + "/3)\n" +
                        "🔐 لطفاً پسورد صحیح را وارد کنید:");
                }
            } else {
                sendMessage(chatId, "🔐 ابتدا دستور /start بزنید.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in authentication: " + e.getMessage());
        }
    }
    
    private void sendMessage(String chatId, String message) {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String data = "chat_id=" + chatId + "&text=" + URLEncoder.encode(message, "UTF-8");

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(data);
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("✅ Message sent to " + chatId);
            }
            
            conn.getInputStream().close();
        } catch (Exception e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
        }
    }
}

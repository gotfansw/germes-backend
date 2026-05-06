package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QrCodeService {

    private static final int MIN_SIZE = 64;
    private static final int MAX_SIZE = 1024;


    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();


    public String generateSvg(String text, int size) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст для QR-кода не может быть пустым");
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Размер QR должен быть от " + MIN_SIZE + " до " + MAX_SIZE + " пикселей, получено: " + size);
        }

        String cacheKey = text + "|" + size;
        return cache.computeIfAbsent(cacheKey, k -> generate(text, size));
    }

    private String generate(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    // [FIX #13] Передаём 0,0 — ZXing выберет минимальный размер матрицы
                    // для данного контента. SVG масштабируется через viewBox — пикселизации нет.
                    0,
                    0,
                    Map.of(
                            EncodeHintType.MARGIN, 1,
                            EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
                    )
            );
            return toSvg(matrix, size);
        } catch (WriterException e) {
            throw new IllegalStateException("Не удалось сгенерировать QR для: " + text, e);
        }
    }

    private String toSvg(BitMatrix matrix, int displaySize) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();

        StringBuilder path = new StringBuilder();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (matrix.get(x, y)) {
                    path.append("M").append(x).append(",").append(y).append("h1v1h-1z");
                }
            }
        }

        // viewBox задаёт логическое пространство (размер матрицы),
        // width/height — физический размер в браузере.
        // SVG масштабирует автоматически — QR всегда чёткий при любом displaySize.
        return "<svg xmlns=\"http://www.w3.org/2000/svg\"" +
                " viewBox=\"0 0 " + w + " " + h + "\"" +
                " shape-rendering=\"crispEdges\"" +
                " width=\"" + displaySize + "\"" +
                " height=\"" + displaySize + "\">" +
                "<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>" +
                "<path d=\"" + path + "\" fill=\"#000\"/>" +
                "</svg>";
    }
}
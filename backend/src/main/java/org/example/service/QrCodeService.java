package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class QrCodeService {

    public String generateSvg(String text, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    size,
                    size,
                    Map.of(EncodeHintType.MARGIN, 1)
            );
            return toSvg(matrix, size, size);
        } catch (WriterException e) {
            throw new IllegalStateException("Не удалось сгенерировать QR", e);
        }
    }

    private String toSvg(BitMatrix matrix, int width, int height) {
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y)) {
                    path.append("M").append(x).append(",").append(y).append("h1v1h-1z");
                }
            }
        }

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + matrix.getWidth() + " " + matrix.getHeight() +
                "\" shape-rendering=\"crispEdges\" width=\"" + width + "\" height=\"" + height + "\">" +
                "<rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>" +
                "<path d=\"" + path + "\" fill=\"#000\"/>" +
                "</svg>";
    }
}

package com.myscript.iink.getstarted;

public interface QRCodeFoundListener {
    void onQRCodeFound(String qrCode);

    void qrCodeNotFound();
}

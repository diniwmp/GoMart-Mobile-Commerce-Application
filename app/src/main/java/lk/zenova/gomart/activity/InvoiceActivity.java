package lk.zenova.gomart.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.zenova.gomart.R;
import lk.zenova.gomart.databinding.ActivityInvoiceBinding;
import lk.zenova.gomart.model.Order;
import lk.zenova.gomart.model.Product;

public class InvoiceActivity extends AppCompatActivity {

    private ActivityInvoiceBinding binding;
    private FirebaseFirestore db;
    private String orderId;
    private Order currentOrder;
    private double shippingCost = 300;

    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");

        loadInvoice();

        binding.invoiceBtnDownload.setOnClickListener(v -> checkPermissionAndDownload());

        binding.invoiceBtnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.invoiceShareBtn.setOnClickListener(v ->
                Toast.makeText(this, "Download first then share from Files", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadInvoice() {
        db.collection("orders")
                .whereEqualTo("orderId", orderId)
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds.isEmpty()) {
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentOrder = qds.getDocuments().get(0).toObject(Order.class);
                    if (currentOrder == null) return;

                    populateInvoice();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load invoice", Toast.LENGTH_SHORT).show()
                );
    }

    private void populateInvoice() {
        String shortId = orderId.length() > 8
                ? "#" + orderId.substring(orderId.length() - 8) : "#" + orderId;
        binding.invoiceOrderId.setText(shortId);

        if (currentOrder.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            binding.invoiceDate.setText(sdf.format(currentOrder.getOrderDate().toDate()));
        }

        binding.invoiceStatus.setText(currentOrder.getStatus());

        boolean hasBillingAddress =
                currentOrder.getBillingAddress() != null
                        && currentOrder.getBillingAddress().getAddress() != null
                        && !currentOrder.getBillingAddress().getAddress().trim().isEmpty();

        Order.Address displayAddress = hasBillingAddress
                ? currentOrder.getBillingAddress()
                : currentOrder.getShippingAddress();

        if (displayAddress != null) {
            binding.invoiceShippingName.setText(
                    displayAddress.getName() != null ? displayAddress.getName() : "");
            binding.invoiceShippingContact.setText(
                    displayAddress.getContact() != null ? displayAddress.getContact() : "");
            binding.invoiceShippingAddress.setText(
                    displayAddress.getAddress() != null ? displayAddress.getAddress() : "");
        }

        loadOrderItems();
    }

    private void loadOrderItems() {
        if (currentOrder.getOrderItems() == null
                || currentOrder.getOrderItems().isEmpty()) return;

        List<String> productIds = new ArrayList<>();
        currentOrder.getOrderItems().forEach(
                item -> productIds.add(item.getProductId()));

        db.collection("products")
                .whereIn("productId", productIds)
                .get()
                .addOnSuccessListener(qds -> {
                    Map<String, Product> productMap = new HashMap<>();
                    qds.getDocuments().forEach(ds -> {
                        Product p = ds.toObject(Product.class);
                        if (p != null)
                            productMap.put(p.getProductId(), p);
                    });

                    double subtotal = 0;

                    for (Order.OrderItem item
                            : currentOrder.getOrderItems()) {

                        Product product =
                                productMap.get(item.getProductId());

                        String title = product != null
                                ? product.getTitle()
                                : item.getProductId();

                        String imageUrl = null;
                        if (product != null
                                && product.getImages() != null
                                && !product.getImages().isEmpty()) {
                            imageUrl = product.getImages().get(0);
                        }

                        double lineTotal =
                                item.getUnitPrice() * item.getQuantity();
                        subtotal += lineTotal;

                        addItemRow(
                                title,
                                imageUrl,
                                item.getQuantity(),
                                item.getUnitPrice(),
                                lineTotal);
                    }

                    double total = subtotal + shippingCost;
                    binding.invoiceSubtotal.setText(
                            String.format(Locale.US,
                                    "LKR %,.2f", subtotal));
                    binding.invoiceShippingCost.setText(
                            String.format(Locale.US,
                                    "LKR %,.2f", shippingCost));
                    binding.invoiceTotal.setText(
                            String.format(Locale.US,
                                    "LKR %,.2f", total));
                });
    }

    private void addItemRow(String title, String imageUrl,
                            int qty, double unitPrice,
                            double lineTotal) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_invoice,
                        binding.invoiceItemsContainer, false);

        android.widget.ImageView imgView =
                row.findViewById(R.id.invoice_row_image);
        TextView tvTitle  = row.findViewById(R.id.invoice_row_title);
        TextView tvQty    = row.findViewById(R.id.invoice_row_qty);
        TextView tvPrice  = row.findViewById(R.id.invoice_row_price);
        TextView tvTotal  = row.findViewById(R.id.invoice_row_total);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.shopping_cart_24)
                    .centerCrop()
                    .into(imgView);
        } else {
            imgView.setImageResource(R.drawable.shopping_cart_24);
        }

        tvTitle.setText(title);
        tvQty.setText("Qty: " + qty);

        tvPrice.setText(String.format(Locale.US,
                "LKR %,.2f", unitPrice));

        tvTotal.setText(String.format(Locale.US,
                "LKR %,.2f", lineTotal));

        binding.invoiceItemsContainer.addView(row);
    }


    private void checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generateAndSavePDF();
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                generateAndSavePDF();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            generateAndSavePDF();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateAndSavePDF() {
        View invoiceView = binding.invoiceContent;

        invoiceView.measure(
                View.MeasureSpec.makeMeasureSpec(
                        invoiceView.getWidth(),
                        View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        0, View.MeasureSpec.UNSPECIFIED));
        invoiceView.layout(0, 0,
                invoiceView.getMeasuredWidth(),
                invoiceView.getMeasuredHeight());

        int width  = invoiceView.getMeasuredWidth();
        int height = invoiceView.getMeasuredHeight();

        if (width <= 0 || height <= 0) {
            Toast.makeText(this,
                    "Invoice not ready yet, please wait",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(android.graphics.Color.WHITE);
        invoiceView.draw(canvas);

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(
                        width, height, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        page.getCanvas().drawBitmap(bitmap, 0, 0, new Paint());
        pdfDocument.finishPage(page);

        bitmap.recycle();

        String fileName = "Invoice_"
                + orderId.substring(0, Math.min(8, orderId.length()))
                + ".pdf";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentValues values =
                        new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME,
                        fileName);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE,
                        "application/pdf");
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 1);

                android.net.Uri collection =
                        android.provider.MediaStore.Downloads.getContentUri(
                                android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY);
                android.net.Uri itemUri =
                        getContentResolver().insert(collection, values);

                if (itemUri == null) {
                    pdfDocument.close();
                    Toast.makeText(this,
                            "Failed to create file in Downloads",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                try (java.io.OutputStream os =
                             getContentResolver().openOutputStream(itemUri)) {
                    pdfDocument.writeTo(os);
                }
                pdfDocument.close();

                values.clear();
                values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(itemUri, values, null, null);

                Toast.makeText(this,
                        "Saved to Downloads: " + fileName,
                        Toast.LENGTH_LONG).show();

            } else {
                File downloadsDir =
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();

                File file = new File(downloadsDir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                pdfDocument.writeTo(fos);
                fos.close();
                pdfDocument.close();

                sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        android.net.Uri.fromFile(file)));

                Toast.makeText(this,
                        "Saved to Downloads: " + fileName,
                        Toast.LENGTH_LONG).show();
                Log.i("PDF", "Saved to: " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            pdfDocument.close();
            Log.e("PDF", "Save failed: " + e.getMessage());
            Toast.makeText(this,
                    "Failed to save: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
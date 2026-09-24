package com.eazybytes.eazystore.service;

import com.eazybytes.eazystore.entity.Product;
import com.eazybytes.eazystore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIShoppingService {

    private final ProductRepository productRepository;
    private final GeminiService geminiService;

    public String getShoppingRecommendation(String userMessage) {

        List<Product> products = productRepository.findAll();

        String productContext = products.stream()
                .map(product -> String.format(
                        "Product ID: %d%nName: %s%nDescription: %s%nPrice: $%s%nPopularity: %d%nImage URL: %s",
                        product.getId(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getPopularity(),
                        product.getImageUrl()
                ))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                You are Eazy AI, a helpful shopping assistant for an e-commerce website called EasyStore.

                The user is looking for products from the EasyStore catalog.

                IMPORTANT RULES:
                1. Recommend ONLY products from the product catalog provided below.
                2. Never invent a product, price, availability, or product ID.
                3. If no product matches the user's request, clearly say that no matching product was found.
                4. Keep the response concise and friendly.
                5. If the user mentions a budget, respect that budget.
                6. If the user asks about a product, use the catalog information.
                7. Do not make up information that is not present in the catalog.

                EASY STORE PRODUCT CATALOG:
                %s

                USER REQUEST:
                %s
                """.formatted(productContext, userMessage);

        return geminiService.askGemini(prompt);
    }
}

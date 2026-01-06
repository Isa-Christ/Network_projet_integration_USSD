package com.network.projet.ussd.util;

import com.network.projet.ussd.repository.UssdServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ShortCodeGenerator - Generate and validate USSD short codes
 * 
 * Features:
 * - Auto-increment based on existing services
 * - Duplicate validation
 * - Multi-operator support (configurable prefix)
 * - Format validation
 * 
 * Examples:
 * - Default: *500*1#, *500*2#, *500*3#...
 * - Custom: *777*1# (Orange), *678*1# (Camtel)
 * 
 * @author Magne Isabelle Christ
 * @since 2026-01-06
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortCodeGenerator {
    
    private final UssdServiceRepository service_repository;
    
    @Value("${ussd.shortcode.prefix:*500*}")
    private String default_prefix;
    
    @Value("${ussd.shortcode.suffix:#}")
    private String default_suffix;
    
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile("\\*(\\d+)\\*(\\d+)#");
    
    /**
     * Generate next available short code with default prefix
     * 
     * @return Mono<String> Next short code (e.g., "*500*3#")
     */
    public Mono<String> generateNext() {
        return generateNextWithPrefix(default_prefix, default_suffix);
    }
    
    /**
     * Generate next short code with custom prefix/suffix
     * 
     * @param prefix USSD prefix (e.g., "*777*")
     * @param suffix USSD suffix (e.g., "#")
     * @return Mono<String> Next short code
     */
    public Mono<String> generateNextWithPrefix(String prefix, String suffix) {
        return findMaxSequenceNumber(prefix, suffix)
            .map(max_number -> buildShortCode(prefix, max_number + 1, suffix))
            .doOnSuccess(code -> log.info("Generated short code: {}", code));
    }
    
    /**
     * Find the highest sequence number currently in use
     * 
     * @param prefix USSD prefix
     * @param suffix USSD suffix
     * @return Mono<Integer> Maximum sequence number
     */
    private Mono<Integer> findMaxSequenceNumber(String prefix, String suffix) {
        return service_repository.findAll()
            .mapNotNull(service -> {
                String short_code = service.getShortCode();
                if (short_code == null || !short_code.startsWith(prefix)) {
                    return null;
                }
                return extractSequenceNumber(short_code);
            })
            .reduce(0, Integer::max);
    }
    
    /**
     * Extract sequence number from short code
     * 
     * @param short_code Short code (e.g., "*500*42#")
     * @return Sequence number (e.g., 42)
     */
    private Integer extractSequenceNumber(String short_code) {
        Matcher matcher = SHORTCODE_PATTERN.matcher(short_code);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        }
        return 0;
    }
    
    /**
     * Build short code from components
     * 
     * @param prefix USSD prefix
     * @param sequence_number Sequence number
     * @param suffix USSD suffix
     * @return Complete short code
     */
    private String buildShortCode(String prefix, int sequence_number, String suffix) {
        return prefix + sequence_number + suffix;
    }
    
    /**
     * Check if short code is available (not already used)
     * 
     * @param short_code Short code to check
     * @return Mono<Boolean> true if available, false if taken
     */
    public Mono<Boolean> isAvailable(String short_code) {
        if (!isValidFormat(short_code)) {
            log.warn("Invalid short code format: {}", short_code);
            return Mono.just(false);
        }
        
        return service_repository.findByShortCode(short_code)
            .map(service -> false)
            .defaultIfEmpty(true);
    }
    
    /**
     * Validate short code format
     * 
     * @param short_code Short code to validate
     * @return true if valid format
     */
    public boolean isValidFormat(String short_code) {
        if (short_code == null || short_code.isEmpty()) {
            return false;
        }
        
        return SHORTCODE_PATTERN.matcher(short_code).matches();
    }
    
    /**
     * Generate short code for specific operator
     * 
     * @param operator_name Operator name (MTN, ORANGE, CAMTEL, etc.)
     * @return Mono<String> Generated short code
     */
    public Mono<String> generateForOperator(String operator_name) {
        return switch (operator_name.toUpperCase()) {
            case "MTN" -> generateNextWithPrefix("*500*", "#");
            case "ORANGE" -> generateNextWithPrefix("*777*", "#");
            case "CAMTEL" -> generateNextWithPrefix("*678*", "#");
            default -> {
                log.warn("Unknown operator '{}', using default prefix", operator_name);
                yield generateNext();
            }
        };
    }
    
    /**
     * Reserve a specific short code (validate and return if available)
     * 
     * @param short_code Desired short code
     * @return Mono<String> The short code if available, or error
     */
    public Mono<String> reserve(String short_code) {
        if (!isValidFormat(short_code)) {
            return Mono.error(new IllegalArgumentException(
                "Invalid short code format. Expected: *XXX*Y# (e.g., *500*1#)"));
        }
        
        return isAvailable(short_code)
            .flatMap(available -> {
                if (available) {
                    log.info("Short code {} reserved", short_code);
                    return Mono.just(short_code);
                } else {
                    return Mono.error(new IllegalStateException(
                        "Short code " + short_code + " is already in use"));
                }
            });
    }
    
    /**
     * Suggest next N available short codes
     * 
     * @param count Number of suggestions
     * @return Mono<List<String>> List of available short codes
     */
    public Mono<List<String>> suggestNext(int count) {
        return findMaxSequenceNumber(default_prefix, default_suffix)
            .map(max_number -> {
                List<String> suggestions = new ArrayList<>();
                for (int i = 1; i <= count; i++) {
                    String code = buildShortCode(
                        default_prefix, 
                        max_number + i, 
                        default_suffix
                    );
                    suggestions.add(code);
                }
                return suggestions;
            });
    }
    
    /**
     * Parse short code to extract operator and sequence
     * 
     * @param short_code Short code to parse
     * @return ShortCodeInfo Parsed information or null if invalid
     */
    public ShortCodeInfo parse(String short_code) {
        Matcher matcher = SHORTCODE_PATTERN.matcher(short_code);
        
        if (!matcher.matches()) {
            return null;
        }
        
        String operator_code = matcher.group(1);
        int sequence_number = Integer.parseInt(matcher.group(2));
        
        String operator_name = switch (operator_code) {
            case "500" -> "MTN";
            case "777" -> "ORANGE";
            case "678" -> "CAMTEL";
            default -> "UNKNOWN";
        };
        
        return new ShortCodeInfo(operator_name, operator_code, sequence_number, short_code);
    }
    
    /**
     * ShortCodeInfo record - Parsed short code information
     * 
     * @param operator_name Operator name (MTN, ORANGE, CAMTEL, UNKNOWN)
     * @param operator_code Operator code (500, 777, 678, etc.)
     * @param sequence_number Sequence number
     * @param full_code Complete short code
     */
    public record ShortCodeInfo(
        String operator_name,
        String operator_code,
        int sequence_number,
        String full_code
    ) {}
    
    /**
     * Generate range of short codes (for bulk registration)
     * 
     * @param start_sequence Start number
     * @param end_sequence End number
     * @return List of short codes
     */
    public List<String> generateRange(int start_sequence, int end_sequence) {
        List<String> codes = new ArrayList<>();
        
        for (int i = start_sequence; i <= end_sequence; i++) {
            codes.add(buildShortCode(default_prefix, i, default_suffix));
        }
        
        log.info("Generated {} short codes from {} to {}", 
            codes.size(), start_sequence, end_sequence);
        
        return codes;
    }
    
    /**
     * Get configured prefix (useful for frontend display)
     * 
     * @return Default prefix
     */
    public String getDefaultPrefix() {
        return default_prefix;
    }
    
    /**
     * Get configured suffix
     * 
     * @return Default suffix
     */
    public String getDefaultSuffix() {
        return default_suffix;
    }
}
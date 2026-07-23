#ifndef OBLIVION_CRYPTO_H
#define OBLIVION_CRYPTO_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Derives a hardware-bound key using static salt and device attributes.
 * Returns a pointer to a 32-byte allocated buffer. Must be freed via `oblivion_free_key`.
 */
uint8_t* derive_hardware_key(const uint8_t* salt, size_t salt_len);

/**
 * Scrambled lookup table AES / custom block decryption.
 * Performs in-place decryption and zeroizes intermediate memory.
 * Returns 0 on success, -1 on failure.
 */
int32_t whitebox_decrypt(uint8_t* data, size_t len, const uint8_t* key, size_t key_len);

/**
 * Zeroizes and deallocates memory created by `derive_hardware_key`.
 */
void oblivion_free_key(uint8_t* ptr, size_t len);

#ifdef __cplusplus
}
#endif

#endif // OBLIVION_CRYPTO_H

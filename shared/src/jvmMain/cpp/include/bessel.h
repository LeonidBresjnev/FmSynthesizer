#pragma
#include <cmath>

namespace synthesizer {
    double bessel_j_series(int n_order, double x_arg, int terms = 20) {
        if (x_arg == 0.0) {
            return (n_order == 0) ? 1.0 : 0.0;
        }

        // For J_n(x) = (-1)^n * J_(-n)(x) if n is negative, we can map to positive n
        int n_abs = std::abs(n_order);
        double sign = 1.0;
        if (n_order < 0 && (n_order % 2 != 0)) { // J_n = (-1)^n J_{-n}
            // This formula is J_{-n}(x) = (-1)^n J_n(x).
            // So if n_order is -3, J_{-3}(x) = (-1)^{-3} J_3(x) = -J_3(x).
            // The code below calculates for n_abs. If n_order was negative and odd, we flip sign.
            // However, the series below is typically defined for non-negative integer order.
            // It's better to use std::cyl_bessel_j if possible, as it handles these cases.
            // For this simple series, let's assume n_order >= 0 for now or rely on
            // properties for negative n if using a more robust implementation.
            // For simplicity here, let's just calculate for n_abs and adjust later if needed based on
            // known Bessel identities if a full negative n implementation is required.
        }


        double sum = 0.0;
        double term_numerator_x_pow_2k = 1.0; // (-x^2/4)^k part, starts with k=0
        double x_div_2_pow_n = pow(x_arg / 2.0, n_abs);

        for (int k = 0; k < terms; ++k) {
            double k_factorial =  std::tgamma(k+1);
            double nk_gamma_plus_1 = std::tgamma(static_cast<double>(n_abs + k) + 1.0); // (n+k)!

            if (k_factorial == 0 || nk_gamma_plus_1 == 0) { // Avoid division by zero
                // This might happen if factorial or tgamma returns 0 for some invalid input
                // or if they are not robust for very large numbers leading to overflow then zero.
                break;
            }

            double term = term_numerator_x_pow_2k / (k_factorial * nk_gamma_plus_1);
            sum += term;

            // Update for next iteration: (-x^2/4)
            term_numerator_x_pow_2k *= (-(x_arg * x_arg) / 4.0);
        }

        double result = x_div_2_pow_n * sum;

        // Apply sign correction for negative odd n if J_{-n}(x) = (-1)^n J_n(x) was the identity used
        // and we computed J_{|n|}(x)
        if (n_order < 0 && (n_order % 2 != 0)) { // If n was negative and odd
            // If the series computes J_{|n|}(x), and J_n(x) = (-1)^n J_{|n|}(x) for n<0
            // (This identity is J_{-m}(x) = (-1)^m J_m(x) )
            // Example: J_{-1}(x) = -J_1(x). If n_order = -1, n_abs = 1. Series computes J_1(x). Result needs sign flip.
            result *= -1.0;
        }


        return result;
    }
}
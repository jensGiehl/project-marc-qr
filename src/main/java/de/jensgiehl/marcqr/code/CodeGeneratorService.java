package de.jensgiehl.marcqr.code;

import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CodeGeneratorService {

    private static final BigInteger ONE = BigInteger.ONE;
    private final SecureRandom random;

    public CodeGeneratorService() {
        this(new SecureRandom());
    }

    CodeGeneratorService(SecureRandom random) {
        this.random = random;
    }

    public BigInteger countPossibilities(CodeGenerationRequest request) {
        Alphabet alphabet = Alphabet.from(request.characters());
        return groups(request, alphabet).stream()
                .map(CodeGroup::size)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public List<String> generate(CodeGenerationRequest request) {
        Alphabet alphabet = Alphabet.from(request.characters());
        List<CodeGroup> groups = groups(request, alphabet);
        BigInteger total = groups.stream().map(CodeGroup::size).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger requested = BigInteger.valueOf(request.count());
        if (requested.compareTo(total) > 0) {
            throw new CodeValidationException(
                    "Es wurden %,d Codes angefordert, aber mit dieser Auswahl sind nur %s eindeutige Codes möglich."
                            .formatted(request.count(), total.toString()));
        }

        Set<BigInteger> sampledIndexes = sampleWithoutReplacement(total, request.count());
        List<String> result = new ArrayList<>(request.count());
        for (BigInteger index : sampledIndexes) {
            result.add(unrank(index, request.length(), alphabet, groups));
        }
        return result;
    }

    private List<CodeGroup> groups(CodeGenerationRequest request, Alphabet alphabet) {
        List<CodeGroup> groups = new ArrayList<>();
        for (int digitCount = 0; digitCount <= request.maxDigits(); digitCount++) {
            if ((digitCount > 0 && alphabet.digits().isEmpty())
                    || (request.length() - digitCount > 0 && alphabet.letters().isEmpty())) {
                continue;
            }
            BigInteger positionChoices = binomial(request.length(), digitCount);
            BigInteger digitChoices = BigInteger.valueOf(alphabet.digits().size()).pow(digitCount);
            BigInteger letterChoices = BigInteger.valueOf(alphabet.letters().size())
                    .pow(request.length() - digitCount);
            BigInteger size = positionChoices.multiply(digitChoices).multiply(letterChoices);
            if (size.signum() > 0) {
                groups.add(new CodeGroup(digitCount, size));
            }
        }
        return groups;
    }

    private Set<BigInteger> sampleWithoutReplacement(BigInteger populationSize, int sampleSize) {
        Set<BigInteger> result = new HashSet<>(Math.max(16, sampleSize * 2));
        BigInteger start = populationSize.subtract(BigInteger.valueOf(sampleSize));
        for (int offset = 0; offset < sampleSize; offset++) {
            BigInteger current = start.add(BigInteger.valueOf(offset));
            BigInteger candidate = randomBelow(current.add(ONE));
            if (!result.add(candidate)) {
                result.add(current);
            }
        }
        return result;
    }

    private BigInteger randomBelow(BigInteger exclusiveUpperBound) {
        BigInteger candidate;
        do {
            candidate = new BigInteger(exclusiveUpperBound.bitLength(), random);
        } while (candidate.compareTo(exclusiveUpperBound) >= 0);
        return candidate;
    }

    private String unrank(BigInteger globalIndex, int length, Alphabet alphabet, List<CodeGroup> groups) {
        BigInteger index = globalIndex;
        for (CodeGroup group : groups) {
            if (index.compareTo(group.size()) < 0) {
                return unrankGroup(index, length, group.digitCount(), alphabet);
            }
            index = index.subtract(group.size());
        }
        throw new IllegalStateException("Code-Index außerhalb des gültigen Bereichs.");
    }

    private String unrankGroup(BigInteger index, int length, int digitCount, Alphabet alphabet) {
        int letterCount = length - digitCount;
        BigInteger assignmentsPerPositionSet = BigInteger.valueOf(alphabet.digits().size()).pow(digitCount)
                .multiply(BigInteger.valueOf(alphabet.letters().size()).pow(letterCount));
        BigInteger[] parts = index.divideAndRemainder(assignmentsPerPositionSet);
        boolean[] digitPositions = unrankCombination(length, digitCount, parts[0]);
        BigInteger assignmentIndex = parts[1];

        char[] code = new char[length];
        for (int position = length - 1; position >= 0; position--) {
            List<Character> source = digitPositions[position] ? alphabet.digits() : alphabet.letters();
            BigInteger base = BigInteger.valueOf(source.size());
            BigInteger[] digit = assignmentIndex.divideAndRemainder(base);
            code[position] = source.get(digit[1].intValueExact());
            assignmentIndex = digit[0];
        }
        return new String(code);
    }

    private boolean[] unrankCombination(int length, int selected, BigInteger rank) {
        boolean[] positions = new boolean[length];
        int remaining = selected;
        for (int position = 0; position < length && remaining > 0; position++) {
            BigInteger combinationsWithCurrent = binomial(length - position - 1, remaining - 1);
            if (rank.compareTo(combinationsWithCurrent) < 0) {
                positions[position] = true;
                remaining--;
            } else {
                rank = rank.subtract(combinationsWithCurrent);
            }
        }
        return positions;
    }

    static BigInteger binomial(int n, int k) {
        if (k < 0 || k > n) {
            return BigInteger.ZERO;
        }
        int effectiveK = Math.min(k, n - k);
        BigInteger result = ONE;
        for (int i = 1; i <= effectiveK; i++) {
            result = result.multiply(BigInteger.valueOf(n - effectiveK + i))
                    .divide(BigInteger.valueOf(i));
        }
        return result;
    }

    private record Alphabet(List<Character> digits, List<Character> letters) {

        static Alphabet from(List<String> values) {
            List<Character> digits = values.stream()
                    .map(value -> value.charAt(0))
                    .filter(Character::isDigit)
                    .toList();
            List<Character> letters = values.stream()
                    .map(value -> value.charAt(0))
                    .filter(Character::isLetter)
                    .toList();
            return new Alphabet(digits, letters);
        }
    }

    private record CodeGroup(int digitCount, BigInteger size) {
    }
}

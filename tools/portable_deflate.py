"""Canonical RFC 1951 fixed-Huffman compression, independent of the installed zlib.

Eight previous matches per hash bucket bound memory and work. This is an
asset encoder, not a replacement for the platform's general-purpose compressor.
Only decompression and checksums are delegated to zlib.
"""
import struct
import zlib


LENGTH_BASE = (3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
               35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
LENGTH_BITS = (0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
               3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
DISTANCE_BASE = (1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
                 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
                 8193, 12289, 16385, 24577)
DISTANCE_BITS = tuple(max(0, index // 2 - 1) for index in range(30))


def reverse(value, count):
    return int(f"{value:0{count}b}"[::-1], 2)


LITERALS = tuple((reverse(code, size), size) for code, size in (
    (48 + symbol, 8) if symbol <= 143 else
    (400 + symbol - 144, 9) if symbol <= 255 else
    (symbol - 256, 7) if symbol <= 279 else (192 + symbol - 280, 8)
    for symbol in range(288)))


def table(bases, widths, last):
    result = [None] * (last + 1)
    for index, (base, width) in enumerate(zip(bases, widths)):
        end = bases[index + 1] if index + 1 < len(bases) else last + 1
        for value in range(base, end):
            result[value] = (index, width, value - base)
    return result


LENGTHS = table(LENGTH_BASE, LENGTH_BITS, 258)
DISTANCES = table(DISTANCE_BASE, DISTANCE_BITS, 32768)


def compress(data):
    output = bytearray(b"\x78\x01")
    bits, count = 3, 3  # final block, fixed Huffman (BFINAL=1, BTYPE=01)

    def write(value, width):
        nonlocal bits, count
        bits |= value << count
        count += width
        while count >= 8:
            output.append(bits & 255)
            bits >>= 8
            count -= 8

    previous = [-1] * 65536
    chain = [-1] * 32768
    position, size = 0, len(data)
    while position < size:
        length, candidate = 0, -1
        if position + 2 < size:
            key = ((data[position] * 251 + data[position + 1]) * 251 + data[position + 2]) & 65535
            probe = previous[key]
            for _ in range(8):
                if probe < 0 or position - probe > 32768:
                    break
                limit = min(258, size - position)
                matched = 0
                while matched < limit and data[probe + matched] == data[position + matched]:
                    matched += 1
                if matched > length:
                    length, candidate = matched, probe
                if length == limit:
                    break
                probe = chain[probe & 32767]
        if length >= 3:
            index, width, extra = LENGTHS[length]
            write(*LITERALS[257 + index])
            write(extra, width)
            index, width, extra = DISTANCES[position - candidate]
            write(reverse(index, 5), 5)
            write(extra, width)
        else:
            length = 1
            write(*LITERALS[data[position]])
        for current in range(position, min(position + length, size - 2)):
            key = ((data[current] * 251 + data[current + 1]) * 251 + data[current + 2]) & 65535
            chain[current & 32767] = previous[key]
            previous[key] = current
        position += length
    write(*LITERALS[256])
    if count:
        output.append(bits & 255)
    output.extend(struct.pack(">I", zlib.adler32(data)))
    return bytes(output)

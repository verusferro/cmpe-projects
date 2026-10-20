# Calling convention
# ------------------
#   Callee-saved across calls : r12, r13, r14, r15, rbx  (used as globals)
#   Scratch / caller-saved    : rax, rcx, rdx, rsi, rdi, r8, r9, r10, r11
#
# Global register usage (live across the whole program)
# -----------------------------------------------------
#   r12  -- read cursor: pointer to next unread byte in in_buf
#   r13  -- end-of-input pointer (in_buf + bytes_read)
#   r14  -- process counter used during parsing
#   r15  -- simulation clock (current cycle index) used in schedulers
# =============================================================================


# -----------------------------------------------------------------------------
# .bss  --  zero-initialised uninitialised data
# -----------------------------------------------------------------------------
.bss

# Input / output character buffers
.lcomm  in_buf,    4096   # raw bytes read from stdin
.lcomm  out_buf,   2048   # one byte per simulated cycle (max output is 1024)
.lcomm  tok_buf,     64   # scratch: holds the current whitespace-delimited token

# Per-process parallel arrays, indexed 0 .. nproc-1.
# The spec caps the process count at 10, so 16 slots is more than enough.
# Each slot is 8 bytes (one qword) except p_id which is 1 byte per slot.
.lcomm  p_id,        16   # process letter IDs ('A'..'Z')
.lcomm  p_remain,   128   # remaining burst time (decremented during simulation)
.lcomm  p_arrival,  128   # arrival time (cycle when the process first appears)
.lcomm  p_priority, 128   # priority value (smaller = higher priority, PF only)

# Round-Robin scheduling queue (append-only with monotonic head/tail indices).
# Worst case (quantum = 1) needs about nproc + 1024 entries; sized to 2048
# qwords for headroom.
.lcomm  rr_queue, 16384

# Scalar state variables
.lcomm  in_len,       8   # number of bytes returned by the read syscall
.lcomm  nproc,        8   # total number of processes parsed from input
.lcomm  out_len,      8   # number of characters written into out_buf so far
.lcomm  quantum,      8   # RR time quantum


.text
.globl  _start


# _start  --  program entry point
_start:
        # ------------------------------------------------------------------
        # syscall: read(fd=0, buf=in_buf, count=4096)
        # rax=0 -> sys_read, rdi=0 -> stdin, rsi=buffer ptr, rdx=max bytes
        # On return rax holds the number of bytes actually read.
        # ------------------------------------------------------------------
        movq    $0,  %rax                   # syscall number: sys_read
        movq    $0,  %rdi                   # file descriptor: stdin (0)
        leaq    in_buf(%rip), %rsi          # destination buffer
        movq    $4096, %rdx                 # maximum bytes to read
        syscall
        movq    %rax, in_len(%rip)          # save actual byte count

        # ------------------------------------------------------------------
        # Set up r12 (cursor) and r13 (one-past-end) for the input stream.
        # All parsing helpers advance r12 and check r12 < r13.
        # ------------------------------------------------------------------
        leaq    in_buf(%rip), %r12          # r12 = pointer to first input byte
        movq    in_len(%rip), %r13          # r13 = byte count (temporary)
        addq    %r12, %r13                  # r13 = pointer to one past last byte

        # Read the algorithm name (first token) into tok_buf.
        call    skip_spaces
        call    read_token

        # ------------------------------------------------------------------
        # Dispatch on the command token.
        #
        # The spec guarantees the algorithm name is one of FCFS / SJF /
        # SRTF / PF / RR.  Their first bytes are unique except for 'S',
        # whose second byte ('J' = SJF, otherwise SRTF) disambiguates.
        # ------------------------------------------------------------------
        leaq    tok_buf(%rip), %rdi         # rdi -> command token
        movb    (%rdi), %al                 # al = first byte
        cmpb    $'F', %al
        je      .Ldo_fcfs
        cmpb    $'P', %al
        je      .Ldo_pf
        cmpb    $'S', %al
        je      .Ldo_s
        # The only remaining algorithm starts with 'R' (RR).
        call    parse_rr_input
        call    run_rr
        jmp     .Lemit

.Ldo_fcfs:
        call    parse_fcfs_input
        call    run_fcfs
        jmp     .Lemit

.Ldo_pf:
        call    parse_pf_input
        call    run_pf
        jmp     .Lemit

.Ldo_s:
        cmpb    $'J', 1(%rdi)               # 'SJ...' -> SJF, else SRTF
        je      .Ldo_sjf
        call    parse_srtf_input
        call    run_srtf
        jmp     .Lemit
.Ldo_sjf:
        call    parse_sjf_input
        call    run_sjf
        # fall through to .Lemit

        # ------------------------------------------------------------------
        # .Lemit -- write out_buf then a single newline to stdout, then exit.
        # ------------------------------------------------------------------
.Lemit:
        # Write the simulation result string (out_len bytes from out_buf).
        movq    $1, %rax                    # syscall: write
        movq    $1, %rdi                    # file descriptor: stdout (1)
        leaq    out_buf(%rip), %rsi         # pointer to output buffer
        movq    out_len(%rip), %rdx         # number of bytes to write
        syscall

        # Write a trailing newline character (byte value 10).
        # Re-use the start of out_buf as a 1-byte scratch buffer.
        movb    $10, out_buf(%rip)          # store '\n' at out_buf[0]
        movq    $1, %rax                    # syscall: write
        movq    $1, %rdi                    # stdout
        leaq    out_buf(%rip), %rsi         # address of the '\n' byte
        movq    $1, %rdx                    # write exactly 1 byte
        syscall

        movq    $60, %rax                   # syscall: exit
        xorq    %rdi, %rdi                  # exit code 0 (success)
        syscall


# =============================================================================
# skip_spaces
# Advances r12 past spaces and trailing newlines.  Per the spec, tokens are
# separated by a single space; an LF may appear at end of input (read from
# stdin), so we treat both as whitespace.  Stops at r13 (end of input) or a
# non-whitespace byte.
# Clobbers: al
# =============================================================================
skip_spaces:
.Lss_loop:
        cmpq    %r13, %r12
        jge     .Lss_done                   # at or past end of input
        movb    (%r12), %al
        cmpb    $' ', %al
        je      .Lss_step
        cmpb    $10,  %al                   # LF
        je      .Lss_step
        jmp     .Lss_done
.Lss_step:
        incq    %r12
        jmp     .Lss_loop
.Lss_done:
        ret


# =============================================================================
# read_token
# Copies bytes from in_buf (starting at r12) into tok_buf, stopping at the
# next space, newline, or end of input.  NUL-terminates the token.
# Returns: rax = number of bytes copied (0 if no more tokens)
# Clobbers: rax, rcx, rdi
# =============================================================================
read_token:
        leaq    tok_buf(%rip), %rdi
        xorq    %rcx, %rcx                  # rcx = byte count
.Lrt_loop:
        cmpq    %r13, %r12
        jge     .Lrt_done
        movb    (%r12), %al
        cmpb    $' ', %al
        je      .Lrt_done
        cmpb    $10,  %al
        je      .Lrt_done
        movb    %al, (%rdi,%rcx)
        incq    %rcx
        incq    %r12
        jmp     .Lrt_loop
.Lrt_done:
        movb    $0, (%rdi,%rcx)             # NUL-terminate
        movq    %rcx, %rax                  # return length
        ret


# =============================================================================
# parse_int_field
# Parses a decimal integer from [rsi], advancing rsi past the digits.
# If the FIRST byte at rsi is '-', that one leading '-' is consumed first
# (it is the field separator between token parts, e.g. the '-' between burst
# and arrival).  After that leading skip, parsing reads decimal digits and
# stops at the next '-', NUL, or any non-digit character.
# Returns: rax = parsed integer value (0 if field is empty)
# Clobbers: rax, rcx, rsi
# =============================================================================
parse_int_field:
        xorq    %rax, %rax                  # rax = accumulator, start at 0
        xorq    %rcx, %rcx                  # rcx = current byte (zero-extended)

        # Skip exactly one leading '-' separator if present.
        movb    (%rsi), %cl
        cmpb    $'-', %cl
        jne     .Lpi_loop                   # no leading dash -> go straight to digits
        incq    %rsi                        # consume the one leading '-'

        # Read decimal digits until NUL or any non-digit (including '-').
.Lpi_loop:
        movb    (%rsi), %cl                 # load current byte into cl
        testb   %cl, %cl
        jz      .Lpi_done                   # NUL terminator -> done
        subb    $'0', %cl                   # convert ASCII digit to numeric value
        cmpb    $9, %cl
        ja      .Lpi_done                   # value > 9: not a digit (e.g. '-') -> stop
        imulq   $10, %rax                   # shift accumulator left one decimal place
        addq    %rcx, %rax                  # add new digit
        incq    %rsi                        # advance past this digit
        jmp     .Lpi_loop
.Lpi_done:
        ret


# =============================================================================
# parse_fcfs_input / parse_srtf_input
# Tokens of the form  <ID>-<burst>-<arrival>  (2 dashes).
# Fills p_id, p_remain, p_arrival for each process; sets nproc.
# parse_srtf_input is an alias -- the token format is identical to FCFS.
# =============================================================================
parse_fcfs_input:
        xorq    %r14, %r14                  # r14 = process index
.Lpf_next:
        call    skip_spaces
        cmpq    %r13, %r12
        jge     .Lpf_done                   # end of input -> done
        call    read_token
        testq   %rax, %rax
        jz      .Lpf_done                   # no more tokens -> done

        leaq    tok_buf(%rip), %rsi         # rsi -> tok_buf[0] (the ID byte)
        movb    (%rsi), %al                 # ID letter
        leaq    p_id(%rip), %rdi
        movb    %al, (%rdi,%r14)
        incq    %rsi                        # rsi -> '-'; parse_int_field will consume it

        call    parse_int_field             # burst
        leaq    p_remain(%rip), %rdi
        movq    %rax, (%rdi,%r14,8)

        call    parse_int_field             # arrival
        leaq    p_arrival(%rip), %rdi
        movq    %rax, (%rdi,%r14,8)

        incq    %r14
        jmp     .Lpf_next
.Lpf_done:
        movq    %r14, nproc(%rip)
        ret

parse_srtf_input:
        jmp     parse_fcfs_input


# =============================================================================
# parse_sjf_input
# Tokens of the form  <ID>-<burst>  (1 dash).  All processes arrive at t=0;
# we leave p_arrival entries at zero (the .bss is already zero-initialised).
# =============================================================================
parse_sjf_input:
        xorq    %r14, %r14
.Lps_next:
        call    skip_spaces
        cmpq    %r13, %r12
        jge     .Lps_done
        call    read_token
        testq   %rax, %rax
        jz      .Lps_done

        leaq    tok_buf(%rip), %rsi
        movb    (%rsi), %al                 # ID letter
        leaq    p_id(%rip), %rdi
        movb    %al, (%rdi,%r14)
        incq    %rsi                        # rsi -> '-'

        call    parse_int_field             # burst
        leaq    p_remain(%rip), %rdi
        movq    %rax, (%rdi,%r14,8)

        incq    %r14
        jmp     .Lps_next
.Lps_done:
        movq    %r14, nproc(%rip)
        ret


# =============================================================================
# parse_pf_input
# Tokens of the form  <ID>-<burst>-<arrival>-<priority>  (3 dashes).
# =============================================================================
parse_pf_input:
        xorq    %r14, %r14
.Lppf_next:
        call    skip_spaces
        cmpq    %r13, %r12
        jge     .Lppf_done
        call    read_token
        testq   %rax, %rax
        jz      .Lppf_done

        leaq    tok_buf(%rip), %rsi
        movb    (%rsi), %al                 # ID letter
        leaq    p_id(%rip), %rdi
        movb    %al, (%rdi,%r14)
        incq    %rsi                        # rsi -> '-'

        call    parse_int_field             # burst
        leaq    p_remain(%rip), %rdi
        movq    %rax, (%rdi,%r14,8)

        call    parse_int_field             # arrival
        leaq    p_arrival(%rip), %rdi
        movq    %rax, (%rdi,%r14,8)

        call    parse_int_field             # priority
        leaq    p_priority(%rip), %rdi
        movq    %rax, (%rdi,%r14,8)

        incq    %r14
        jmp     .Lppf_next
.Lppf_done:
        movq    %r14, nproc(%rip)
        ret


# =============================================================================
# parse_rr_input
# Reads RR's process tokens followed by the quantum value as the LAST token:
#
#     RR <ID>-<burst> <ID>-<burst> ... <Quantum>
#
# Process IDs are uppercase letters (>= 'A' = 0x41) while the quantum is a
# decimal integer (digits 0x30..0x39).  A single comparison on the first
# byte therefore distinguishes the two: bytes < 'A' are the quantum.
# =============================================================================
parse_rr_input:
        xorq    %r14, %r14                  # r14 = process index
.Lprr_next:
        call    skip_spaces
        cmpq    %r13, %r12
        jge     .Lprr_done
        call    read_token
        testq   %rax, %rax
        jz      .Lprr_done

        leaq    tok_buf(%rip), %rsi
        movb    (%rsi), %al
        cmpb    $'A', %al
        jl      .Lprr_quantum               # first byte < 'A' -> digit -> quantum

        # Process token: <ID>-<burst>.
        leaq    p_id(%rip), %rdi
        movb    %al, (%rdi,%r14)
        incq    %rsi                        # rsi -> '-'
        call    parse_int_field             # burst
        leaq    p_remain(%rip), %rdi
        movq    %rax, (%rdi,%r14,8)
        incq    %r14
        jmp     .Lprr_next

.Lprr_quantum:
        # Per the spec the quantum is the LAST token, so this is the final
        # iteration; we fall through to .Lprr_done after storing it.
        call    parse_int_field
        movq    %rax, quantum(%rip)
.Lprr_done:
        movq    %r14, nproc(%rip)
        ret


# =============================================================================
# emit_char
# Appends the byte in al to out_buf and increments out_len.
# Clobbers: rdi, rsi  (out_len loaded/stored; no other registers touched)
# =============================================================================
emit_char:
        movq    out_len(%rip), %rdi         # rdi = current write index
        leaq    out_buf(%rip), %rsi         # rsi -> base of output buffer
        movb    %al, (%rsi,%rdi)            # out_buf[out_len] = al
        incq    %rdi                        # out_len++
        movq    %rdi, out_len(%rip)         # save updated index
        ret


# =============================================================================
# all_done
# Returns 1 in rax if every process has p_remain == 0, else 0.
# Used by schedulers as their termination condition.
# Clobbers: rax, rbx, rcx, rdi
# =============================================================================
all_done:
        movq    nproc(%rip), %rcx           # rcx = total number of processes
        xorq    %rbx, %rbx                  # rbx = loop index
        leaq    p_remain(%rip), %rdi        # rdi -> p_remain array base
.Lad_loop:
        cmpq    %rcx, %rbx
        jge     .Lad_yes                    # visited all processes -> all done
        movq    (%rdi,%rbx,8), %rax         # rax = p_remain[rbx]
        testq   %rax, %rax
        jnz     .Lad_no                     # non-zero remain -> not done yet
        incq    %rbx
        jmp     .Lad_loop
.Lad_yes:
        movq    $1, %rax                    # return 1: all finished
        ret
.Lad_no:
        xorq    %rax, %rax                  # return 0: work remains
        ret


# =============================================================================
# run_fcfs  --  First Come First Served (non-preemptive)
# Selection rule: among all arrived processes (p_arrival <= r15), pick the one
# with the smallest arrival time.  Ties broken by array order (first found
# wins because we use strict <).  Once selected, a process runs to completion
# without interruption.
# r15 = simulation clock (current cycle), r9 = index of chosen process.
# =============================================================================
run_fcfs:
        xorq    %r15, %r15                  # r15 = clock, start at cycle 0
.Lfc_outer:
        call    all_done
        testq   %rax, %rax
        jnz     .Lfc_end                    # all processes finished -> stop

        movq    nproc(%rip), %rcx           # rcx = process count
        movq    $-1, %r9                    # r9 = best candidate index (-1 = none)
        movabs  $0x7fffffffffffffff, %r10   # r10 = best arrival seen so far (INT64_MAX)
        xorq    %rbx, %rbx                  # rbx = loop index

        # --- Find the arrived process with the earliest arrival time ---
.Lfc_find:
        cmpq    %rcx, %rbx
        jge     .Lfc_chose                  # checked all processes

        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%rbx,8), %rax
        testq   %rax, %rax
        jz      .Lfc_skip                   # p_remain == 0 -> already finished

        leaq    p_arrival(%rip), %rdi
        movq    (%rdi,%rbx,8), %rax         # rax = p_arrival[rbx]
        cmpq    %r10, %rax
        jge     .Lfc_skip                   # arrival >= current best -> skip
        movq    %rax, %r10                  # new best arrival time
        movq    %rbx, %r9                   # new best candidate index
.Lfc_skip:
        incq    %rbx
        jmp     .Lfc_find

.Lfc_chose:
        # If earliest arrival is still in the future, emit idle cycles ('X').
        cmpq    %r10, %r15                  # is clock < earliest arrival?
        jge     .Lfc_run                    # clock >= arrival -> ready to run
        movb    $'X', %al
        call    emit_char                   # one idle cycle
        incq    %r15                        # advance clock
        jmp     .Lfc_outer

.Lfc_run:
        # Run the selected process to completion (non-preemptive).
        leaq    p_id(%rip), %rdi
        movb    (%rdi,%r9), %al             # al = process ID letter
        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%r9,8), %rdx          # rdx = remaining burst cycles
.Lfc_run_loop:
        testq   %rdx, %rdx
        jz      .Lfc_run_done               # emitted all burst cycles
        call    emit_char                   # emit one cycle for this process
        incq    %r15                        # advance clock
        decq    %rdx                        # one fewer remaining cycle
        jmp     .Lfc_run_loop
.Lfc_run_done:
        leaq    p_remain(%rip), %rdi
        movq    $0, (%rdi,%r9,8)            # mark process as finished (remain = 0)
        jmp     .Lfc_outer
.Lfc_end:
        ret


# =============================================================================
# run_sjf  --  Shortest Job First (non-preemptive, all arrive at t=0)
# Selection rule: among all arrived processes (p_arrival <= r15; all are 0),
# pick the one with the smallest remaining burst.  Ties broken by array order.
# Once selected a process runs to completion without interruption.
# =============================================================================
run_sjf:
        xorq    %r15, %r15
.Lsj_outer:
        call    all_done
        testq   %rax, %rax
        jnz     .Lsj_end

        movq    nproc(%rip), %rcx
        movq    $-1, %r9                    # r9 = best candidate (-1 = none yet)
        movabs  $0x7fffffffffffffff, %r10   # r10 = shortest burst seen (INT64_MAX)
        xorq    %rbx, %rbx

        # --- Find arrived process with shortest remaining burst ---
.Lsj_find:
        cmpq    %rcx, %rbx
        jge     .Lsj_chose

        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%rbx,8), %rax         # rax = p_remain[rbx]
        testq   %rax, %rax
        jz      .Lsj_skip                   # already finished

        leaq    p_arrival(%rip), %rdi
        movq    (%rdi,%rbx,8), %rdx         # rdx = p_arrival[rbx]
        cmpq    %r15, %rdx
        jg      .Lsj_skip                   # hasn't arrived yet

        cmpq    %r10, %rax
        jge     .Lsj_skip                   # burst >= current best -> skip
        movq    %rax, %r10                  # new shortest burst
        movq    %rbx, %r9
.Lsj_skip:
        incq    %rbx
        jmp     .Lsj_find

.Lsj_chose:
        cmpq    $-1, %r9
        jne     .Lsj_run
        movb    $'X', %al                   # no process ready -> idle cycle
        call    emit_char
        incq    %r15
        jmp     .Lsj_outer

.Lsj_run:
        leaq    p_id(%rip), %rdi
        movb    (%rdi,%r9), %al             # al = ID letter of chosen process
        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%r9,8), %rdx          # rdx = burst cycles remaining
.Lsj_run_loop:
        testq   %rdx, %rdx
        jz      .Lsj_run_done
        call    emit_char
        incq    %r15
        decq    %rdx
        jmp     .Lsj_run_loop
.Lsj_run_done:
        leaq    p_remain(%rip), %rdi
        movq    $0, (%rdi,%r9,8)            # mark process finished
        jmp     .Lsj_outer
.Lsj_end:
        ret


# =============================================================================
# run_srtf  --  Shortest Remaining Time First (preemptive SJF)
# Each cycle: among all arrived processes pick the one with least p_remain.
# The chosen process runs for exactly 1 cycle, then selection repeats.
# This allows a newly arrived shorter job to preempt the running one.
# =============================================================================
run_srtf:
        xorq    %r15, %r15
.Lsr_loop:
        call    all_done
        testq   %rax, %rax
        jnz     .Lsr_end

        movq    nproc(%rip), %rcx
        movq    $-1, %r9
        movabs  $0x7fffffffffffffff, %r10   # r10 = smallest p_remain seen so far
        xorq    %rbx, %rbx

        # --- Find arrived process with shortest remaining time ---
.Lsr_find:
        cmpq    %rcx, %rbx
        jge     .Lsr_chose

        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%rbx,8), %rax         # rax = p_remain[rbx]
        testq   %rax, %rax
        jz      .Lsr_next                   # already finished

        leaq    p_arrival(%rip), %rdi
        movq    (%rdi,%rbx,8), %rdx         # rdx = p_arrival[rbx]
        cmpq    %r15, %rdx
        jg      .Lsr_next                   # not yet arrived

        cmpq    %r10, %rax
        jge     .Lsr_next                   # remaining >= best -> skip
        movq    %rax, %r10                  # new best remaining time
        movq    %rbx, %r9                   # new best candidate
.Lsr_next:
        incq    %rbx
        jmp     .Lsr_find

.Lsr_chose:
        cmpq    $-1, %r9
        jne     .Lsr_run
        movb    $'X', %al                   # no process ready -> idle
        call    emit_char
        incq    %r15
        jmp     .Lsr_loop

.Lsr_run:
        leaq    p_id(%rip), %rdi
        movb    (%rdi,%r9), %al             # al = ID letter
        call    emit_char                   # emit exactly 1 cycle (preemptive)
        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%r9,8), %rax
        decq    %rax                        # p_remain[r9]--
        movq    %rax, (%rdi,%r9,8)
        incq    %r15                        # clock++
        jmp     .Lsr_loop                   # re-select next cycle
.Lsr_end:
        ret


# =============================================================================
# run_pf  --  Priority-First (preemptive)
# Each cycle: among arrived processes pick the one with the lowest priority
# number (lower number = higher priority).  Ties in priority are broken by
# shortest remaining burst; ties in both are broken by array order.
# =============================================================================
run_pf:
        xorq    %r15, %r15
.Lpr_loop:
        call    all_done
        testq   %rax, %rax
        jnz     .Lpr_end

        movq    nproc(%rip), %rcx
        movq    $-1,  %r9                   # r9  = best candidate index
        movabs  $0x7fffffffffffffff, %r10   # r10 = best (lowest) priority seen
        movabs  $0x7fffffffffffffff, %r11   # r11 = remaining burst of best candidate
        xorq    %rbx, %rbx

.Lpr_find:
        cmpq    %rcx, %rbx
        jge     .Lpr_chose

        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%rbx,8), %rax         # rax = p_remain[rbx]
        testq   %rax, %rax
        jz      .Lpr_next                   # already finished

        leaq    p_arrival(%rip), %rdi
        movq    (%rdi,%rbx,8), %rdx         # rdx = p_arrival[rbx]
        cmpq    %r15, %rdx
        jg      .Lpr_next                   # not yet arrived

        leaq    p_priority(%rip), %rdi
        movq    (%rdi,%rbx,8), %r8          # r8 = p_priority[rbx]

        cmpq    %r10, %r8
        jl      .Lpr_take                   # strictly lower priority number -> take
        jg      .Lpr_next                   # higher priority number -> skip

        # Equal priority: break tie by shorter remaining burst
        cmpq    %r11, %rax
        jge     .Lpr_next                   # burst >= current best -> skip
.Lpr_take:
        movq    %r8,  %r10                  # update best priority
        movq    %rax, %r11                  # update best remaining burst
        movq    %rbx, %r9                   # update best candidate index
.Lpr_next:
        incq    %rbx
        jmp     .Lpr_find

.Lpr_chose:
        cmpq    $-1, %r9
        jne     .Lpr_run
        movb    $'X', %al                   # no process ready -> idle
        call    emit_char
        incq    %r15
        jmp     .Lpr_loop

.Lpr_run:
        leaq    p_id(%rip), %rdi
        movb    (%rdi,%r9), %al             # al = ID letter
        call    emit_char                   # run 1 cycle (preemptive)
        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%r9,8), %rax
        decq    %rax
        movq    %rax, (%rdi,%r9,8)          # p_remain[r9]--
        incq    %r15                        # clock++
        jmp     .Lpr_loop
.Lpr_end:
        ret


# =============================================================================
# run_rr  --  Round Robin
# Processes are served in input order, each for up to `quantum` cycles.
# A process that does not finish its quantum slice is appended to the back of
# the queue.  A process that finishes before its full quantum causes idle 'X'
# padding so that each queue slot always accounts for exactly `quantum` output
# cycles.
#
# Data structures:
#   rr_queue[] -- flat array of process indices (grows as procs are requeued)
#   rbx        -- current queue length (tail index; grows when re-queuing)
#   r14        -- dequeue index (head); advances by 1 each turn
# =============================================================================
run_rr:
        # Initialise rr_queue with process indices 0, 1, ..., nproc-1.
        leaq    rr_queue(%rip), %rdi        # rdi -> queue base
        movq    nproc(%rip), %rcx           # rcx = process count
        xorq    %rbx, %rbx                  # rbx = write index / queue length
.Lrri_init:
        cmpq    %rcx, %rbx
        jge     .Lrri_init_done
        movq    %rbx, (%rdi,%rbx,8)         # rr_queue[rbx] = rbx (index == value)
        incq    %rbx
        jmp     .Lrri_init
.Lrri_init_done:
        xorq    %r14, %r14                  # r14 = head index, start at 0

.Lrr_turn:
        cmpq    %rbx, %r14
        jge     .Lrr_end                    # head reached tail -> queue exhausted

        leaq    rr_queue(%rip), %rdi
        movq    (%rdi,%r14,8), %r9          # r9 = process index for this turn
        incq    %r14                        # advance head

        # Determine actual slice length: min(p_remain[r9], quantum).
        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%r9,8), %rax          # rax = p_remain[r9]
        movq    quantum(%rip), %rdx         # rdx = quantum
        cmpq    %rdx, %rax
        jge     .Lrr_active_set             # remain >= quantum -> run full quantum
        movq    %rax, %rdx                  # remain < quantum -> run only what's left
.Lrr_active_set:
        # rdx = cycles to emit for this process this turn.

        leaq    p_id(%rip), %rdi
        movb    (%rdi,%r9), %r8b            # r8b = process ID letter
        movq    %rdx, %rcx                  # rcx = countdown for this slice
.Lrr_print_id:
        testq   %rcx, %rcx
        jz      .Lrr_id_done
        movb    %r8b, %al
        call    emit_char                   # emit one cycle of this process
        decq    %rcx
        jmp     .Lrr_print_id
.Lrr_id_done:

        # Deduct the cycles just run from p_remain[r9].
        leaq    p_remain(%rip), %rdi
        movq    (%rdi,%r9,8), %rax          # reload (emit_char clobbers rdi)
        subq    %rdx, %rax                  # new remaining = old remaining - cycles run
        movq    %rax, (%rdi,%r9,8)          # store back

        testq   %rax, %rax
        jnz     .Lrr_requeue                # still has remaining time -> re-queue

        # Process finished within this quantum slot.
        # Pad with 'X' for any unused cycles in the slot so that each slot
        # is always exactly `quantum` output characters wide.
        movq    quantum(%rip), %rcx
        subq    %rdx, %rcx                  # rcx = quantum - cycles_used (padding needed)
.Lrr_pad:
        testq   %rcx, %rcx
        jz      .Lrr_pad_done
        movb    $'X', %al
        call    emit_char                   # emit one idle padding cycle
        decq    %rcx
        jmp     .Lrr_pad
.Lrr_pad_done:
        jmp     .Lrr_turn                   # next queue entry, no re-queue

.Lrr_requeue:
        # Append this process index to the back of the queue for another turn later.
        leaq    rr_queue(%rip), %rdi
        movq    %r9, (%rdi,%rbx,8)          # rr_queue[rbx] = r9
        incq    %rbx                        # extend tail
        jmp     .Lrr_turn

.Lrr_end:
        ret

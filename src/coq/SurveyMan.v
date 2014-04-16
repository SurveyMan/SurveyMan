Require Import Coq.Sorting.Permutation.
(* model columns as a set *)

Inductive id := 
| Id : nat -> id.

Inductive option := 
| Instructional : option
| Option : id -> nat -> option.

Inductive flag :=
| Exclusive : bool -> flag
| Randomize : bool -> flag
| Ordered : bool -> flag
| Freetext : bool -> flag.

Inductive flags :=
| Flags : flag -> flag -> flag -> flag -> flags.

Inductive block :=
| Top : nat -> block
| Sub : nat -> bool -> block -> block.

(* will need to capture that Dest can only use constructor Top *)
Inductive branch :=
| Null : branch
| Next : branch
| Dest : block -> branch.

Inductive question := 
| Question : id -> nat -> flags -> block -> branch -> question.

Inductive answer :=
| Answer : question -> option -> answer.

Definition randomize (q : list question) : list question := 

(* an answer set satisfies a question set if it describes a valid, complete path *)
(* when is a path complete? when we've reached the last question  -- this doesn't hold for breakoff *)
Inductive satisfies (a : list answer) (b : list question) : Prop := 
| True.


(* goal to prove : given a ground truth set of responses, these responses are invariant under randomization - use permutation here *)

Theorem answer_invariant : forall Q A,
                             satisfies A Q -> satisfies A (randomize Q).
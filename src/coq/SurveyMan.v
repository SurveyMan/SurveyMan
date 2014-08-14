Require Import Coq.Sorting.Permutation.
Require Import RegExp.Definitions.
(* model columns as a set *)

Inductive id := 
| Id : nat -> id.

Inductive option := 
| Instructional : option
| Option : id -> nat -> option.

Inductive freetext :=
| Freetext : bool -> freetext
| Default : string -> freetext
| Validated : RegExp -> freetext.

Inductive flag :=
| Exclusive : bool -> flag
| Randomize : bool -> flag
| Ordered : bool -> flag.

Inductive flags :=
| Flags : flag -> flag -> flag -> flags.

Inductive block :=
| Top : id -> branch -> block
| Sub : id -> bool -> block -> block
with  branch :=
| Null : branch
| Next : branch
| Dest : block -> branch.

Inductive question := 
| Question : id  -> flags -> block -> branch -> question.

Inductive answer :=
| Answer : question -> option -> answer.

Definition randomize (q : list question) : list question := 
  q.

(* an answer set satisfies a question set if it describes a valid, complete path *)
(* when is a path complete? when we've reached the last question  -- this doesn't hold for breakoff *)
Inductive satisfies (a : list answer) (b : list question) : Prop := 
| True.


(* goal to prove : given a ground truth set of responses, these responses are invariant under randomization - use permutation here *)

Theorem answer_invariant : forall Q A,
                             satisfies A Q -> satisfies A (randomize Q).
Admitted.
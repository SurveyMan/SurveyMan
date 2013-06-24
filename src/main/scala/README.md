# CSV Grammar

A survey is comprised of a header row and subsequent entry rows:


```
	<survey> ::= <header> "\n" <entries>
```

### `<header>`

The header contains at a minumum the columns named "QUESTION" and "OPTIONS". The other semantically important columns have the following defaults:

* BLOCK - When there is no block column, all questions are assumed to be in the same block.
* RESOURCE - If there is no resource (i.e. an external file) named, this is an empty string and will not be used.
* EXCLUSIVE - The default is "true"/"yes", which visually corresponds to a radio button. 
* ORDERED - The default is "false"/"no". This is used with the PERTURB column to determine how answer options may be shuffled, if shuffling is permitted. It is also used for quality control.
* PERTURB - The default is "true"/"yes". This indicates that a question's answer options may be shuffled.
* BRANCH - The default is empty; if branching is specified, then it must be a 2-tuple of block identifiers.

Note that literals are in caps, but capitalization does not matter.

Headers may contain other columns. If the column name is not specified, then a unique identifier is generated for it. Columns must be order-insensitive.

### `<entries>`

All rows are separated by the newline character ("\n"). Cells in an entry are separated by commans. Entry rows are order-sensitive.

Most cells (column types) can be expressed as a regex. 

```
	BLOCK ::= [0-9](.[0-9])*
	RESOURCE ::= <url> | ""
	EXCLUSIVE ::= "true" | "false" | "yes" | "no" | ""
	ORDERED ::= "true" | "false" | "yes" | "no" | ""
	PERTURB ::= "true" | "false" | "yes" | "no" | ""
	BRANCH ::= "true" | "false" | "yes" | "no" | ""
```

Question and Options have more complex structure.

### Special characters

Quotes may be delimited by " (UTF-8 : 0x22) or two ' (UTF-8 : 0x27). The first of these tokens found begins a quoted block. If the other token is found, it is ignored during tokenization; ensuring properly formed quotations is done after all entries have been tokenized. All quotation marks may be escaped by \ (UTF-8 : 0x5C). The " (UTF-8 0x22) may be escaped by preceding it with another " (UTF-8 0x22), provided that the lexer is currently inside a quotation block.
cd docs
latex newman-tosch && bibtex newman-tosch && latex newman-tosch && pdflatex newman-tosch && open newman-tosch.pdf
rm newman-tosch.aux newman-tosch.bbl newman-tosch.dvi newman-tosch.log newman-tosch.blg
cd ..

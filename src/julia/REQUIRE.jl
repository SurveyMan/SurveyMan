# I had an error after install Julia on a new machine where ~/.julia existed, but nothing was initialized.
# Adding the init call as a hack. See https://github.com/JuliaLang/julia/issues/4587
Pkg.init() 
Pkg.add("Distributions")
Pkg.add("HypothesisTests")
Pkg.add("DataFrames")
Pkg.clone("https://github.com/shirlenator/Plotly.jl")
Pkg.add("Gadfly")
# Pkg.add("Cairo")


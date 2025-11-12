test af augmented og normal graf består umiddelbaret (bare lige en test med en enkelt query)
java -jar app/build/libs/app.jar query denmark-augmented.graph 1125179421 1126400131
distance=8309 relaxed=1051 time(ns)=4272500

java -jar app/build/libs/app.jar query-raw denmark.graph 1125179421 1126400131 
distance=8309 relaxed=1208019 time(ns)=478797667

afstand er ens, relaxed vertices er markant lavere, og tiden er langt hurtigere- (godt tegn)
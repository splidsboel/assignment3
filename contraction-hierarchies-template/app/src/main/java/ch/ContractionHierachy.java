package ch;

public class ContractionHierachy {

/*
 * Notes:
 * Top-Down Nested Dissection approach, where we want to split the graph G into two disjunct graphs G1 and G2, where S is the separtor that splits the graph, such that S + G1 + G2 = G. 
 * We want S to contain as few vertices as possible, and G1 and G2 to be of approximately similar size (balanced)
 * This separation is done recursively until the graphs are small enough (how small that is is to be seen...)
 * Then, vertices are eliminated in each of the two subgraphs, and then in the separator (Cholevsky decomposition??)
 * Final output is a reorderd graph (with hopefully few "shortcuts")
 */

    public ContractionHierachy() {
      // To be filled out
    }

    public Result<Integer> query(long s, long t) {
        
        // To be filled out

        return new Result<Integer>(0,0,0); 
    }

    public void storeGraph() {
        // To be filled out
    }
}

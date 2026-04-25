package com.team05.fooddelivery.order.model.neo4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;


@Node("User")
public class UserNode {
    @Id
    @GeneratedValue
    private Long userId;

    private String name;

    @Relationship(type = "ORDERED_FROM", direction = Relationship.Direction.OUTGOING)
    private List<OrderedFrom> orderedFroms = new ArrayList<OrderedFrom>();

    public UserNode() {
    }

    public UserNode(String name) {
        this.name = name;
    }


    public Long getUserId() { return userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<OrderedFrom> getOrderedFroms() { return orderedFroms; }
    public void setOrderedFroms(List<OrderedFrom> orderedFroms) { this.orderedFroms = orderedFroms; }
}

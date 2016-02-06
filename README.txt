CS 144 Project Part 3

Design Choices

We are creating one index where each document represents an item. Each document will have three fields: ItemID, Name, and the concatenation of Name, Category, and Description. We designed the index this way because we need to return ItemID and Name, so we saved them in their own separate field. The concatenation field is used for keyword-based searching.


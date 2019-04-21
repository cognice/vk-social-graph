# vk-social-graph
An example usage of the [graph-layout](https://github.com/cognice/graph-layout) library.
Builds a social graph for the [VK](https://vk.com) user.

The application uses the official [VK API](https://vk.com/dev/manuals) which has a restriction of maximum 3 requests per second. So if you have many friends, it will take some time to dowload their information.
It is also possible to save and load the graph to/from `JSON` files with images encoded in `Base64`.

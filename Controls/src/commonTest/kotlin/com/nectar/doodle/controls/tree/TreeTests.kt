package com.nectar.doodle.controls.tree

import com.nectar.doodle.JsName
import com.nectar.doodle.controls.theme.TreeRenderer
import com.nectar.doodle.controls.theme.TreeRenderer.RowGenerator
import com.nectar.doodle.utils.Path
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.expect

/**
 * Created by Nicholas Eddy on 3/26/18.
 */
class TreeTests {
    @Test @JsName("rootDefaultsToHidden")
    fun `root defaults to hidden`() {
        val tree = Tree<Int, Model<Int>>(model())

        expect(false) { tree.rootVisible     }
        expect(false) { tree.visible(Path()) }
    }

    @Test @JsName("hasRightNumberOfRows")
    fun `has right number of rows`() {
        val root = node(0) { node(1); node(2); node(3) }

        var tree = tree(root)

        expect(3) { tree.numRows }

        expect(1) { tree[Path(0)] }
        expect(2) { tree[Path(1)] }
        expect(3) { tree[Path(2)] }

        tree = tree(root).apply { rootVisible = true }

        expect(4) { tree.numRows }

        expect(0) { tree[Path( )] }
        expect(1) { tree[Path(0)] }
        expect(2) { tree[Path(1)] }
        expect(3) { tree[Path(2)] }
    }

    @Test @JsName("hasRightChildren")
    fun `has right children`() {
        val root = node("root") {
            node("child1") {
                node("child1_1")
                node("child1_2") }
            node("child2") {
                node("child2_1")
            }
            node("child3")
        }

        val uiGenerator = rowGenerator<String>()

        val tree = tree(root, renderer(uiGenerator)).apply { expand(0) }

        expect(5) { tree.numRows }

        expect(true ) { tree.visible(Path(0)    ) }
        expect(true ) { tree.visible(Path(0) + 0) }
        expect(true ) { tree.visible(Path(0) + 1) }
        expect(true ) { tree.visible(Path(1)    ) }
        expect(false) { tree.visible(Path(1) + 0) }
        expect(true ) { tree.visible(Path(2)    ) }

        expect(true ) { tree.visible(0) }
        expect(true ) { tree.visible(1) }
        expect(true ) { tree.visible(2) }
        expect(true ) { tree.visible(3) }
        expect(true ) { tree.visible(4) }

        expect("child1"  ) { tree[Path(0)    ] }
        expect("child1_1") { tree[Path(0) + 0] }
        expect("child1_2") { tree[Path(0) + 1] }
        expect("child2"  ) { tree[Path(1)    ] }
        expect("child2_1") { tree[Path(1) + 0] }
        expect("child3"  ) { tree[Path(2)    ] }
    }

    @Test @JsName("getWorks")
    fun `get path`() {
        validateGetPath(node(11) { node(105); node(-24) { node(33) }; node(0) }, mapOf(
                Path<Int>( )     to  11,
                Path     (0)     to 105,
                Path     (1)     to -24,
                Path     (1) + 0 to  33,
                Path     (2)     to   0))

        validateGetPath(node(11) { node(105); node(-24) { node(33) }; node(0) }, mapOf(
                Path<Int>( )     to  11,
                Path     (0)     to 105,
                Path     (1)     to -24,
                Path     (1) + 0 to  33,
                Path     (2)     to   0)) { expandAll() }
    }

    @Test @JsName("getRow")
    fun `get row`() {
        validateGetRow(node(11) { node(105); node(-24) { node(33) }; node(0) }, listOf(105, -24, 0    ))
        validateGetRow(node(11) { node(105); node(-24) { node(33) }; node(0) }, listOf(105, -24, 33, 0)) {
            expandAll()
        }
        validateGetRow(node(11) { node(105); node(-24) { node(33) }; node(0) }, listOf(11, 105, -24, 33, 0)) {
            rootVisible = true
            expandAll()
        }
    }

    @Test @JsName("expandAll")
    fun `expand all`() {
        val tree = tree(node("root") {
            node("child1") {
                node("child1_1")
                node("child1_2") {
                    node("child1_2_1")
                }
            }
            node("child2") {
                node("child2_1")
            }
            node("child3")
        })

        val observer = mockk<ExpansionObserver<String>>(relaxed = true)

        tree.expanded += observer

        tree.expandAll()

        verify { observer(tree, setOf(Path(0), Path(0) + 0, Path(0) + 1, Path(0) + 1 + 0, Path(1), Path(1) + 0, Path(2))) }
    }

    @Test @JsName("collapseAll")
    fun `collapse all`() {
        val tree = tree(node("root") {
            node("child1") {
                node("child1_1")
                node("child1_2") {
                    node("child1_2_1")
                }
            }
            node("child2") {
                node("child2_1")
            }
            node("child3")
        })

        val observer = mockk<ExpansionObserver<String>>(relaxed = true)

        tree.collapsed += observer

        tree.expandAll  ()
        tree.collapseAll()

        verify { observer(tree, setOf(Path(0), Path(0) + 0, Path(0) + 1, Path(0) + 1 + 0, Path(1), Path(1) + 0, Path(2))) }
    }

    @Test @JsName("expandNonVisible")
    fun `expand non-visible`() {
        val tree = tree(node("root") {
            node("child1") {
                node("child1_1")
                node("child1_2") {
                    node("child1_2_1")
                }
            }
            node("child2") {
                node("child2_1")
            }
            node("child3")
        })

        val observer = mockk<ExpansionObserver<String>>(relaxed = true)

        tree.expanded += observer

        tree.expand(Path(0) + 0 + 1)

        verify { observer(tree, setOf(Path(0) + 0 + 1)) }
    }

    private fun <T> rowGenerator(): RowGenerator<T> = mockk(relaxed = true)

    private fun <T> renderer(uiGenerator: RowGenerator<T> = rowGenerator()): TreeRenderer<T> {
        val ui = mockk<TreeRenderer<T>>(relaxed = true)

        every { ui.generator } returns uiGenerator

        return ui
    }

    private fun <T> tree(root: TreeNode<T>, ui: TreeRenderer<T> = renderer()) = Tree(SimpleModel(root)).apply { renderer = ui }

    private fun <T> validateGetRow(root: TreeNode<T>, expected: List<T>, block: Tree<T, *>.() -> Unit = {}) {
        val tree = tree(root).also { block(it) }

        expected.forEachIndexed { index, value ->
            expect(value) { tree[index] }
        }
    }

    private fun <T> validateGetPath(root: TreeNode<T>, expected: Map<Path<Int>, T>, block: Tree<T, *>.() -> Unit = {}) {
        val tree = tree(root).also{ block(it) }

        expected.forEach { (path, value) ->
            expect(value) { tree[path] }
        }
    }

    private fun <T> model(): Model<T> {
        val result = mockk<Model<T>>(relaxed = true)

        every { result.isEmpty() } returns true

        return result
    }
}

//private fun addChildren(node: NodeBuilder<String>, config: kotlin.collections.List<Int>) {
//    if (config.isNotEmpty()) {
//        (0 until config[0]).forEach { i ->
//            val child = NodeBuilder("${node.value}[$i]") //getRandomText( 20, 100 ) );
//
//            node.children += child
//
//            addChildren(child, config.drop(1))
//        }
//    }
//}